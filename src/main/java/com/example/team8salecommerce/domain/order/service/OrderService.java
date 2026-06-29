package com.example.team8salecommerce.domain.order.service;

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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    // 장바구니 검증 및 주문 생성
    @Transactional
    public OrderResponse createOrder(Long memberId, CreateOrderRequest request) {
        List<Long> cartItemIds = validateAndNormalizeCartItemIds(request);
        Member member = findMember(memberId);
        Cart cart = findCart(memberId);
        List<CartItem> cartItems = findSelectedCartItems(cart.getId(), cartItemIds);
        Map<Long, Product> lockedProducts = lockProducts(cartItems);

        Long totalPrice = calculateTotalPrice(cartItems, lockedProducts);
        Order order = orderRepository.save(
                Order.create(member, totalPrice, LocalDateTime.now())
        );

        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> OrderItem.create(
                        order,
                        lockedProducts.get(cartItem.getProduct().getId()),
                        cartItem.getQuantity()
                ))
                .toList();

        List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);

        // 주문된 항목은 장바구니 조회에서 다시 노출되지 않도록 소프트 삭제
        cartItems.forEach(CartItem::delete);

        return OrderResponse.of(order, savedOrderItems);
    }


    // 인증 회원의 일반 주문 목록을 최신순으로 조회
    @Transactional(readOnly = true)
    public OrderListResponse getOrders(Long memberId) {
        findMember(memberId);
        List<Order> orders = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(memberId);

        if (orders.isEmpty()) {
            return new OrderListResponse(List.of());
        }

        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .toList();

        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findAllByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));

        List<OrderResponse> responses = orders.stream()
                .map(order -> OrderResponse.of(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of())
                ))
                .toList();

        return new OrderListResponse(responses);
    }

    // 회원 본인의 결제 대기 주문을 취소하고 일반 상품 재고를 복구
    @Transactional
    public OrderResponse cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 상태 검증을 먼저 수행하여 취소할 수 없는 주문에는 재고 변경이 일어나지 않게 함
        order.cancel();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);
        Map<Long, Product> lockedProducts = lockProductsByOrderItems(orderItems);

        orderItems.forEach(orderItem -> {
            Long productId = orderItem.getProduct().getId();
            Product product = lockedProducts.get(productId);
            int restoredRows = orderProductRepository.restoreStock(
                    product.getId(),
                    orderItem.getQuantity()
            );

            if (restoredRows == 0) {
                throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
            }
        });

        return OrderResponse.of(order, orderItems);
    }

     // 요청의 장바구니 상품 ID를 검증하고 중복 없는 입력 순서로 정규화
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

    // 회원 조회
    private Member findMember(Long memberId) {
        if (memberId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 회원 소유 장바구니 조회
    private Cart findCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));
    }


    // 요청한 모든 장바구니 상품이 활성 상태이며 회원 장바구니에 속하는지 확인
    private List<CartItem> findSelectedCartItems(Long cartId, List<Long> cartItemIds) {
        List<CartItem> foundItems = cartItemRepository.findActiveCartItemsByIds(cartId, cartItemIds);
        Map<Long, CartItem> itemById = foundItems.stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));

        if (itemById.size() != cartItemIds.size()) {
            throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 저장 및 응답 순서를 클라이언트가 보낸 ID 순서와 동일하게 유지한다.
        return cartItemIds.stream()
                .map(itemById::get)
                .toList();
    }


    // 장바구니 상품의 원본 상품을 잠그고 상품 ID 맵으로 반환
    private Map<Long, Product> lockProducts(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        List<Product> products = orderProductRepository.findAllActiveByIdInForUpdate(productIds);
        validateAllProductsFound(productIds, products);
        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }


    // 취소 대상 주문 상품의 원본 상품을 잠그고 상품 ID 맵으로 반환
    private Map<Long, Product> lockProductsByOrderItems(List<OrderItem> orderItems) {
        List<Long> productIds = orderItems.stream()
                .map(orderItem -> orderItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        List<Product> products = orderProductRepository.findAllByIdInForUpdate(productIds);
        validateAllProductsFound(productIds, products);
        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }


    // 삭제되었거나 누락된 상품이 있는지 검증
    private void validateAllProductsFound(Collection<Long> productIds, List<Product> products) {
        if (products.size() != productIds.size()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }


    // 주문 총액을 계산하면서 각 상품 재고를 차감
    private Long calculateTotalPrice(List<CartItem> cartItems, Map<Long, Product> products) {
        long totalPrice = 0L;

        for (CartItem cartItem : cartItems) {
            Product product = products.get(cartItem.getProduct().getId());
            int decreasedRows = orderProductRepository.decreaseStock(
                    product.getId(),
                    cartItem.getQuantity()
            );

            if (decreasedRows == 0) {
                throw new CustomException(ErrorCode.OUT_OF_STOCK);
            }

            totalPrice += product.getPrice() * cartItem.getQuantity();
        }

        return totalPrice;
    }
}
