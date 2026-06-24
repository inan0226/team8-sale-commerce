package com.example.team8salecommerce.domain.cart.dto.response;


import com.example.team8salecommerce.domain.cart.entity.CartItem;
// 장바구니 상품 조회 응답
public record CartItemDetailResponse(
        Long cartItemId,
        Long productId,
        String productName,
        Long price,
        int quantity,
        Long totalPrice
) {
    public static CartItemDetailResponse from(
            CartItem cartItem
    ) {
        Long totalPrice =
                cartItem.getProduct().getPrice()
                        * cartItem.getQuantity();

        return new CartItemDetailResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity(),
                totalPrice
        );
    }
}
