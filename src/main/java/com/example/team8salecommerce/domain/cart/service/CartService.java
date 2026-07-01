package com.example.team8salecommerce.domain.cart.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.request.UpdateCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.response.CartItemDetailResponse;
import com.example.team8salecommerce.domain.cart.dto.response.CartItemResponse;
import com.example.team8salecommerce.domain.cart.dto.response.CartResponse;
import com.example.team8salecommerce.domain.cart.entity.Cart;
import com.example.team8salecommerce.domain.cart.entity.CartItem;
import com.example.team8salecommerce.domain.cart.repository.CartItemRepository;
import com.example.team8salecommerce.domain.cart.repository.CartRepository;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;

	/** 장바구니에 상품을 추가하고, 이미 담긴 상품이면 수량을 증가시킨다. */
	@Transactional
	public CartItemResponse addCartItem(Long memberId, AddCartItemRequest request) {
		validateMemberId(memberId);
		Optional<Cart> optionalCart = cartRepository.findByMemberId(memberId);

		// 새 장바구니에는 기존 항목이 없으므로 불필요한 CartItem 조회를 생략한다.
		if (optionalCart.isEmpty()) {
			Member member = findMember(memberId);
			Cart cart = cartRepository.save(Cart.create(member));
			Product product = findActiveProduct(request.productId());
			CartItem cartItem = cartItemRepository.save(CartItem.create(cart, product, request.quantity()));
			return CartItemResponse.from(cartItem);
		}

		Cart cart = optionalCart.get();
		Optional<CartItem> optionalCartItem = cartItemRepository.findByCartIdAndProductIdWithActiveProduct(
			cart.getId(),
			request.productId()
		);
		if (optionalCartItem.isPresent()) {
			CartItem cartItem = optionalCartItem.get();
			if (cartItem.isDeleted()) {
				cartItem.restore(request.quantity());
			} else {
				cartItem.addQuantity(request.quantity());
			}
			return CartItemResponse.from(cartItem);
		}

		Product product = findActiveProduct(request.productId());
		CartItem cartItem = cartItemRepository.save(CartItem.create(cart, product, request.quantity()));
		return CartItemResponse.from(cartItem);
	}

	/** 회원의 활성 장바구니 항목과 총 금액을 조회한다. */
	public CartResponse getCart(Long memberId) {
		validateMemberId(memberId);
		Optional<Cart> optionalCart = cartRepository.findByMemberId(memberId);
		if (optionalCart.isEmpty()) {
			// 장바구니가 없을 때만 회원을 조회해 MEMBER_NOT_FOUND 응답 규칙을 유지한다.
			findMember(memberId);
			return new CartResponse(null, List.of(), 0L);
		}

		Cart cart = optionalCart.get();
		List<CartItemDetailResponse> items = cartItemRepository.findActiveCartItems(cart.getId())
			.stream()
			.map(CartItemDetailResponse::from)
			.toList();
		long totalPrice = items.stream()
			.mapToLong(CartItemDetailResponse::totalPrice)
			.sum();

		return new CartResponse(cart.getId(), items, totalPrice);
	}

	/** 회원이 소유한 장바구니 상품의 수량을 변경한다. */
	@Transactional
	public CartItemResponse updateCartItemQuantity(
		Long memberId,
		Long cartItemId,
		UpdateCartItemRequest request
	) {
		Cart cart = findCart(memberId);
		CartItem cartItem = findActiveCartItem(cartItemId);
		validateCartOwner(cart, cartItem);
		cartItem.updateQuantity(request.quantity());
		return CartItemResponse.from(cartItem);
	}

	/** 회원이 소유한 장바구니 상품을 소프트 삭제한다. */
	@Transactional
	public void deleteCartItem(Long memberId, Long cartItemId) {
		Cart cart = findCart(memberId);
		CartItem cartItem = findActiveCartItem(cartItemId);
		validateCartOwner(cart, cartItem);
		cartItem.delete();
	}

	/** 식별자가 유효한 회원을 조회한다. */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
	}

	/** 회원 장바구니를 조회한다. 정상 경로에서는 별도 회원 조회를 수행하지 않는다. */
	private Cart findCart(Long memberId) {
		validateMemberId(memberId);
		Optional<Cart> optionalCart = cartRepository.findByMemberId(memberId);
		if (optionalCart.isPresent()) {
			return optionalCart.get();
		}

		// 장바구니가 없는 실패 경로에서만 회원 존재 여부를 추가로 확인한다.
		findMember(memberId);
		throw new CustomException(ErrorCode.CART_NOT_FOUND);
	}

	/** 삭제되지 않은 장바구니 상품을 조회한다. */
	private CartItem findActiveCartItem(Long cartItemId) {
		return cartItemRepository.findByIdAndDeletedAtIsNull(cartItemId)
			.orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
	}

	/** 삭제되지 않은 판매 상품을 조회한다. */
	private Product findActiveProduct(Long productId) {
		return productRepository.findByIdAndIsDeletedFalse(productId)
			.orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	/** 장바구니 상품이 요청 회원의 장바구니에 속하는지 검증한다. */
	private void validateCartOwner(Cart cart, CartItem cartItem) {
		if (!cartItem.getCart().getId().equals(cart.getId())) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}
	}

	/** 인증 회원 식별자가 누락된 요청을 차단한다. */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}
}
