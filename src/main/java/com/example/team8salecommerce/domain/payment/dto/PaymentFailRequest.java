package com.example.team8salecommerce.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 결제 실패 처리 요청 DTO
 *
 * 사용자가 PortOne 결제에 실패했을 때
 * 클라이언트가 서버에 실패 정보를 전달하는 요청 값이다.
 *
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record PaymentFailRequest(

	/**
	 * 결제 실패 처리할 특가 주문 ID
	 */
	@NotNull(message = "주문 ID는 필수입니다.")
	Long orderId,

	/**
	 * PortOne 결제 ID
	 *
	 * 실패한 결제도 외부 결제 식별자를 저장해서
	 * 중복 실패 처리나 추적에 사용할 수 있게 한다.
	 */
	@NotBlank(message = "PortOne 결제 ID는 필수입니다.")
	String portOnePaymentId,

	/**
	 * 결제 시도 금액
	 *
	 * 서버에 저장된 주문 금액과 일치하는지 검증한다.
	 */
	@NotNull(message = "결제 금액은 필수입니다.")
	@Positive(message = "결제 금액은 0보다 커야 합니다.")
	Long amount,

	/**
	 * 결제 실패 사유
	 *
	 * 예: 카드 한도 초과, 사용자 결제 취소, PG 승인 실패 등
	 */
	@NotBlank(message = "결제 실패 사유는 필수입니다.")
	String failureReason
) {
}
