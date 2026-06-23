package com.example.team8salecommerce.domain.promotion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 특가 상품 선착순 구매 요청 DTO
 *
 * 사용자가 특가 상품을 몇 개 구매할지 요청하는 값이다.
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record PromotionPurchaseRequest(

	@NotNull(message = "구매 수량은 필수입니다.")
	@Min(value = 1, message = "구매 수량은 1개 이상이어야 합니다.")
	Integer quantity
) {
}
