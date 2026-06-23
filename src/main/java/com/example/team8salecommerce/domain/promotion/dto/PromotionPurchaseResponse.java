package com.example.team8salecommerce.domain.promotion.dto;

import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;

/**
 * 특가 상품 선착순 구매 응답 DTO
 *
 * 선착순 구매 요청이 성공했을 때 클라이언트에게 반환할 데이터이다.
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record PromotionPurchaseResponse(

	Long orderId,
	Long promotionProductId,
	String productName,
	Integer quantity,
	Long totalPrice,
	String orderStatus,
	Integer remainingEventStock
) {
	/**
	 * 응답 DTO 생성 팩토리 메서드
	 *
	 * Entity를 Controller에서 직접 반환하지 않고,
	 * 필요한 값만 DTO로 변환해서 반환한다.
	 */
	public static PromotionPurchaseResponse of(
		Long orderId,
		PromotionProduct promotionProduct,
		Integer quantity,
		String orderStatus
	) {
		Long totalPrice = promotionProduct.getPromotionPrice() * quantity;

		return new PromotionPurchaseResponse(
			orderId,
			promotionProduct.getId(),
			promotionProduct.getTitle(),
			quantity,
			totalPrice,
			orderStatus,
			promotionProduct.getRemainingEventStock()
		);
	}
}
