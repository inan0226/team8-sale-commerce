package com.example.team8salecommerce.domain.cart.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    // 장바구니에 상품 추가
    // 중복 담기 시 수량 증가
    @Transactional
    public CartItemResponse addCartItem(
            Long memberId,
            AddCartItemRequest request
    ) {

        // 회원 조회
        Member member = getMember(memberId);

        // 회원의 장바구니 조회 (없으면 생성)
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() ->
                            cartRepository.save(
                                    Cart.create(member)
                            )
                        );

        // 상품 조회
        Product product = productRepository.findByIdAndIsDeletedFalse(
                        request.productId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.PRODUCT_NOT_FOUND));

        // 이미 장바구니에 존재하는 상품인지 확인
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(
                        cart.getId(),
                        product.getId()
                )
                .map(existingCartItem -> {

                    // 삭제된 상품이면 복구
                    if (existingCartItem.getDeletedAt() != null) {
                        existingCartItem.restore(request.quantity());
                        return existingCartItem;
                    }

                    // 동일 상품이면 수량 증가
                    existingCartItem.addQuantity(
                            request.quantity()
                    );

                    return existingCartItem;
                })
                .orElseGet(() -> {

                    // 신규 상품이면 CartItem 생성
                    CartItem newCartItem =
                            CartItem.create(
                                    cart,
                                    product,
                                    request.quantity()
                            );

                    return cartItemRepository.save(
                            newCartItem
                    );
                });
        return CartItemResponse.from(cartItem);
    }

    // 장바구니 조회
    // 회원의 장바구니를 조회한 후 장바구니에 담긴 상품 목록과 총 금액을 계산하여 반환
    @Transactional(readOnly = true)
    public CartResponse getCart(Long memberId) {
        // 조회 api이므로 다른 insert를 발생시키는건 X

        // 회원 조회
        getMember(memberId);

        // 장바구니 조회
        Optional<Cart> optionalCart =
                cartRepository.findByMemberId(memberId);

        // 장바구니가 없는 경우 빈 장바구니 반환
        if (optionalCart.isEmpty()) {
            return new CartResponse(
                    null,
                    List.of(),
                    0L
            );
        }
        Cart cart = optionalCart.get();
        // 장바구니 상품 목록 조회
        List<CartItemDetailResponse> items =
                cartItemRepository.findActiveCartItems(cart.getId())
                        .stream()
                        .map(CartItemDetailResponse::from)
                        .toList();

       // 장바구니 총 금액 계산
        Long totalPrice = items.stream()
                .mapToLong(CartItemDetailResponse::totalPrice)
                .sum();
        // 장바구니 응답 생성
        return new CartResponse(
                cart.getId(),
                items,
                totalPrice
        );
    }

    // 장바구니 상품 수량 변경
    @Transactional
    public CartItemResponse updateCartItemQuantity(
            Long memberId,
            Long cartItemId,
            UpdateCartItemRequest request
    ) {
        // 회원 조회
        Member member = getMember(memberId);

        // 회원 장바구니 조회
        Cart cart = getCartByMemberId(member.getId());

        // 장바구니 상품 조회
        CartItem cartItem = getCartItem(cartItemId);

        // 자신의 장바구니 상품인지 검증
        validateCartOwner(cart, cartItem);

        // 수량 변경
        cartItem.updateQuantity(request.quantity());

        // 응답 반환
        return CartItemResponse.from(cartItem);
    }

    // 장바구니 상품 삭제
    @Transactional
    public void deleteCartItem(
            Long memberId,
            Long cartItemId
    ) {
        // 회원 조회
        Member member = getMember(memberId);

        // 회원 장바구니 조회
        Cart cart = getCartByMemberId(member.getId());

        // 장바구니 상품 조회
        CartItem cartItem = getCartItem(cartItemId);

        // 자신의 장바구니 상품인지 검증
        validateCartOwner(cart, cartItem);

        // Soft Delete
        cartItem.delete();
    }


    // 공통 메서드

    /**
     * 회원 조회
     */
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEMBER_NOT_FOUND
                        ));
    }

    /**
     * 회원 장바구니 조회
     */
    private Cart getCartByMemberId(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CART_NOT_FOUND
                        ));
    }

    /**
     * 장바구니 상품 조회
     */
    private CartItem getCartItem(Long cartItemId) {
        return cartItemRepository
                .findByIdAndDeletedAtIsNull(cartItemId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CART_ITEM_NOT_FOUND
                        ));
    }

    /**
     * 장바구니 소유자 검증
     */
    private void validateCartOwner(
            Cart cart,
            CartItem cartItem
    ) {
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN
            );
        }
    }
}
