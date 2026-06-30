package com.example.team8salecommerce.domain.order.dto.response;

import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.entity.OrderItem;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
	Long orderId,
	Long totalPrice,
	OrderStatus status,
	LocalDateTime orderedAt,
	List<OrderItemResponse> items
) {

	public static OrderResponse of(Order order, List<OrderItem> orderItems) {
		List<OrderItemResponse> itemResponses = orderItems.stream()
			.map(OrderItemResponse::from)
			.toList();

		return new OrderResponse(
			order.getId(),
			order.getTotalPrice(),
			order.getStatus(),
			order.getOrderedAt(),
			itemResponses
		);
	}
}
