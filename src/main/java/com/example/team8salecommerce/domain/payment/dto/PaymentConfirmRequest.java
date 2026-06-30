package com.example.team8salecommerce.domain.payment.dto;

import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 결제 승인 요청 DTO
 *
 * 사용자가 PortOne 결제를 완료한 뒤
 * 서버에 결제 승인을 요청할 때 전달하는 값이다.
 *
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record PaymentConfirmRequest(

	@NotNull(message = "주문 ID는 필수입니다.")
	Long orderId,

	@NotBlank(message = "PortOne 결제 ID는 필수입니다.")
	String portOnePaymentId,

	@NotNull(message = "결제 금액은 필수입니다.")
	@Positive(message = "결제 금액은 0보다 커야 합니다.")
	Long amount,

	PaymentOrderType orderType
) {
	/** 유형 누락 시 기존 특가 결제로 처리한다. */
	public PaymentConfirmRequest {
		if (orderType == null) {
			orderType = PaymentOrderType.PROMOTION;
		}
	}

	/** 기존 생성자 호환성을 유지한다. */
	public PaymentConfirmRequest(Long orderId, String portOnePaymentId, Long amount) {
		this(orderId, portOnePaymentId, amount, PaymentOrderType.PROMOTION);
	}
}
