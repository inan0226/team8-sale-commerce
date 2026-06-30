package com.example.team8salecommerce.domain.cart.dto.response;

import java.util.List;

// 장바구니 조회 응답
public record CartResponse(
	Long cartId,
	List<CartItemDetailResponse> items,
	Long totalPrice
) {
}
