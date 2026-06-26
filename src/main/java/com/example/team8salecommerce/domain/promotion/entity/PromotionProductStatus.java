package com.example.team8salecommerce.domain.promotion.entity;

/**
 * 특가 상품 상태
 *
 * READY : 이벤트 시작 전 대기 상태
 * OPEN : 이벤트 진행 중, 구매 가능 상태
 * SOLD_OUT : 이벤트 재고가 모두 소진된 상태
 * CLOSED : 이벤트 종료 상태
 */
public enum PromotionProductStatus {

	READY,
	OPEN,
	SOLD_OUT,
	CLOSED
}
