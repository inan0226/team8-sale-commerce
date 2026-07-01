package com.example.team8salecommerce.domain.order.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.cart.entity.Cart;
import com.example.team8salecommerce.domain.cart.entity.CartItem;
import com.example.team8salecommerce.domain.cart.repository.CartItemRepository;
import com.example.team8salecommerce.domain.cart.repository.CartRepository;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.team8salecommerce.domain.order.dto.response.OrderListResponse;
import com.example.team8salecommerce.domain.order.dto.response.OrderResponse;
import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.entity.OrderItem;
import com.example.team8salecommerce.domain.order.repository.OrderItemRepository;
import com.example.team8salecommerce.domain.order.repository.OrderProductRepository;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final OrderProductRepository orderProductRepository;
	private final MemberRepository memberRepository;

	/** 선택한 장바구니 상품으로 일반 주문을 생성한다. */
	@Transactional
	public OrderResponse createOrder(Long memberId, CreateOrderRequest request) {
		List<Long> cartItemIds = validateAndNormalizeCartItemIds(request);
		Cart cart = findCart(memberId);
		List<CartItem> cartItems = findSelectedCartItems(cart.getId(), cartItemIds);
		long totalPrice = decreaseStockAndCalculateTotalPrice(cartItems);

		Order order = orderRepository.save(
			Order.create(cart.getMember(), totalPrice, LocalDateTime.now())
		);
		List<OrderItem> orderItems = cartItems.stream()
			.map(cartItem -> OrderItem.create(order, cartItem.getProduct(), cartItem.getQuantity()))
			.toList();
		List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);

		// 주문된 장바구니 항목은 엔티티별 갱신 대신 조건부 일괄 UPDATE로 비활성화한다.
		int deletedCount = cartItemRepository.softDeleteActiveByIds(
			cart.getId(),
			cartItemIds,
			LocalDateTime.now()
		);
		if (deletedCount != cartItemIds.size()) {
			throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
		}

		return OrderResponse.of(order, savedOrderItems);
	}

	/** 인증 회원의 일반 주문 목록을 최신순으로 조회한다. */
	public OrderListResponse getOrders(Long memberId) {
		validateMemberId(memberId);
		List<Order> orders = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(memberId);

		if (orders.isEmpty()) {
			// 주문이 없는 경우에만 회원을 조회해 MEMBER_NOT_FOUND 응답 규칙을 유지한다.
			findMember(memberId);
			return new OrderListResponse(List.of());
		}

		List<Long> orderIds = orders.stream()
			.map(Order::getId)
			.toList();
		Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findAllByOrderIdIn(orderIds)
			.stream()
			.collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));
		List<OrderResponse> responses = orders.stream()
			.map(order -> OrderResponse.of(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
			.toList();

		return new OrderListResponse(responses);
	}

	/** 결제 대기 주문을 취소하고 일반 상품 재고를 복구한다. */
	@Transactional
	public OrderResponse cancelOrder(Long memberId, Long orderId) {
		validateMemberId(memberId);
		Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
		order.cancel();

		List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);
		Map<Long, Integer> quantitiesByProductId = aggregateQuantitiesByProductId(orderItems);
		quantitiesByProductId.forEach((productId, quantity) -> {
			int restoredRows = orderProductRepository.restoreStock(productId, quantity);
			if (restoredRows == 0) {
				throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
			}
		});

		return OrderResponse.of(order, orderItems);
	}

	/** 요청의 장바구니 상품 ID를 검증하고 입력 순서대로 정규화한다. */
	private List<Long> validateAndNormalizeCartItemIds(CreateOrderRequest request) {
		if (request == null || request.cartItemIds() == null || request.cartItemIds().isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
		if (request.cartItemIds().stream().anyMatch(id -> id == null || id <= 0)) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(request.cartItemIds());
		if (uniqueIds.size() != request.cartItemIds().size()) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
		return List.copyOf(uniqueIds);
	}

	/** 회원 장바구니를 조회한다. 정상 경로에서는 별도 회원 조회를 수행하지 않는다. */
	private Cart findCart(Long memberId) {
		validateMemberId(memberId);
		Optional<Cart> optionalCart = cartRepository.findByMemberId(memberId);
		if (optionalCart.isPresent()) {
			return optionalCart.get();
		}

		findMember(memberId);
		throw new CustomException(ErrorCode.CART_NOT_FOUND);
	}

	/** 주문 대상 장바구니 상품을 잠금 조회하고 요청 순서로 반환한다. */
	private List<CartItem> findSelectedCartItems(Long cartId, List<Long> cartItemIds) {
		List<CartItem> foundItems = cartItemRepository.findActiveCartItemsByIds(cartId, cartItemIds);
		Map<Long, CartItem> itemById = foundItems.stream()
			.collect(Collectors.toMap(CartItem::getId, Function.identity()));
		if (itemById.size() != cartItemIds.size()) {
			throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
		}

		return cartItemIds.stream()
			.map(itemById::get)
			.toList();
	}

	/** 상품별 주문 수량을 합산해 중복 상품의 재고 UPDATE를 한 번으로 줄이고 총액을 계산한다. */
	private long decreaseStockAndCalculateTotalPrice(List<CartItem> cartItems) {
		long totalPrice = 0L;
		Map<Long, Integer> quantitiesByProductId = new LinkedHashMap<>();

		for (CartItem cartItem : cartItems) {
			Product product = cartItem.getProduct();
			if (product.isDeleted()) {
				throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
			}
			totalPrice += product.getPrice() * cartItem.getQuantity();
			quantitiesByProductId.merge(product.getId(), cartItem.getQuantity(), Integer::sum);
		}

		quantitiesByProductId.forEach((productId, quantity) -> {
			int decreasedRows = orderProductRepository.decreaseStock(productId, quantity);
			if (decreasedRows == 0) {
				throw new CustomException(ErrorCode.OUT_OF_STOCK);
			}
		});
		return totalPrice;
	}

	/** 주문 상품의 상품별 수량을 합산해 재고 복구 UPDATE 횟수를 최소화한다. */
	private Map<Long, Integer> aggregateQuantitiesByProductId(List<OrderItem> orderItems) {
		return orderItems.stream()
			.collect(Collectors.toMap(
				orderItem -> orderItem.getProduct().getId(),
				OrderItem::getQuantity,
				Integer::sum,
				LinkedHashMap::new
			));
	}

	/** 식별자가 유효한 회원을 조회한다. */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
	}

	/** 인증 회원 식별자가 누락된 요청을 차단한다. */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}
}
