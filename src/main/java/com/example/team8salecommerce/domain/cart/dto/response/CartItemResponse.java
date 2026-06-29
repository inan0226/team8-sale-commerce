package com.example.team8salecommerce.domain.cart.dto.response;

import com.example.team8salecommerce.domain.cart.entity.CartItem;

public record CartItemResponse(

        Long cartItemId,
        Long productId,
        String productName,
        Integer quantity
) {
    public static CartItemResponse from(
            CartItem cartItem
    ) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getQuantity()
        );
    }
}
