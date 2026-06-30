package com.example.team8salecommerce.domain.order.dto.response;

import java.util.List;

public record OrderListResponse(
	List<OrderResponse> orders
) {
}
