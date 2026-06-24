package com.example.team8salecommerce.domain.cart.service;

import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
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
        // 회원의 장바구니 조회
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {
                            Member member = memberRepository.findById(memberId)
                                    .orElseThrow(() ->
                                            new CustomException(
                                                    ErrorCode.MEMBER_NOT_FOUND));
                            return cartRepository.save(Cart.create(member));
                        });

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

        // 회원 장바구니 조회
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CART_NOT_FOUND));
        // 장바구니 상품 목록 조회
        List<CartItemDetailResponse> items =
                cartItemRepository.findByCartId(
                                cart.getId())
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
}
