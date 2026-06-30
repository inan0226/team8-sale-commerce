package com.example.team8salecommerce.domain.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * 주문할 장바구니 상품 식별자 목록을 전달하는 요청 DTO다.
 */
public record CreateOrderRequest(
        @NotEmpty(message = "주문할 장바구니 상품을 한 개 이상 선택해야 합니다.")
        List<@Positive(message = "장바구니 상품 ID는 양수여야 합니다.") Long> cartItemIds
) {
}
