package com.example.team8salecommerce.domain.refund.dto;

import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 환불 요청 DTO
 *
 * 사용자가 주문 상세 페이지에서 환불을 요청할 때 전달하는 값이다.
 * orderId는 PathVariable로 받기 때문에 Request Body에는 포함하지 않는다.
 *
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record RefundRequest (

	@NotNull(message = "환불 사유는 필수입니다.")
	RefundReasonType reasonType,

	@Size(max = 500, message = "환불 상세 사유는 500자 이하로 입력해주세요.")
	String reasonDetail
) {
}
