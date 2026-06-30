package com.example.team8salecommerce.domain.order.dto.response;

import com.example.team8salecommerce.domain.order.entity.OrderItem;


public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        Long productPrice,
        Integer quantity,
        Long totalPrice
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getProductName(),
                orderItem.getProductPrice(),
                orderItem.getQuantity(),
                orderItem.calculateTotalPrice()
        );
    }
}
