package com.example.team8salecommerce.domain.payment.entity;

/**
 * 결제가 참조하는 주문 저장소의 유형을 구분한다.
 */
public enum PaymentOrderType {
    /** 일반 장바구니 주문 */
    NORMAL,

    /** 기존 선착순 특가 주문 */
    PROMOTION
}