package com.example.team8salecommerce.domain.refund.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.team8salecommerce.domain.refund.dto.RefundRequest;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.facade.RefundFacade;
import com.example.team8salecommerce.domain.refund.service.RefundQueryService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 환불 Controller
 *
 * 환불 관련 HTTP 요청을 받는 역할을 담당한다.
 */
@Validated
@RestController
@RequiredArgsConstructor
public class RefundController {

	private final RefundFacade refundFacade;
	private final RefundQueryService refundQueryService;
	private final AuthMemberResolver authMemberResolver;

	/**
	 * 환불 요청 API
	 *
	 * 결제 완료된 특가 주문에 대해 환불을 요청한다.
	 * 요청이 성공하면 PortOne 환불, 주문 환불 완료, 이벤트 재고 복구까지 처리한다.
	 */
	@PostMapping("/orders/{orderId}/refunds")
	public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
		@AuthenticationPrincipal AuthMember authMember,
		@Positive(message = "주문 ID는 0보다 커야 합니다.")
		@PathVariable Long orderId,
		@Valid @RequestBody RefundRequest request
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);
		validatePositiveId(orderId);

		RefundResponse responseDto = refundFacade.requestRefund(
			memberId,
			orderId,
			request
		);

		return ResponseEntity.ok(ApiResponse.success(responseDto));
	}

	/**
	 * 환불 상세 조회 API
	 *
	 * 로그인한 회원이 본인의 환불 상세 정보를 조회한다.
	 *
	 * @param authMember 인증된 회원 정보
	 * @param refundId 조회할 환불 ID
	 * @return 환불 상세 응답
	 */
	@GetMapping("/refunds/{refundId}")
	public ResponseEntity<ApiResponse<RefundResponse>> getRefund(
		@AuthenticationPrincipal AuthMember authMember,
		@Positive @PathVariable Long refundId
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);
		validatePositiveId(refundId);

		RefundResponse responseDto = refundQueryService.getRefund(memberId, refundId);

		return ResponseEntity.ok(ApiResponse.success(responseDto));
	}

	/**
	 * 요청 경로 ID를 검증한다.
	 *
	 * standalone MockMvc 환경에서는 @PathVariable의 @Positive 검증이
	 * 기대처럼 동작하지 않을 수 있으므로 Controller에서도 명시적으로 방어한다.
	 */
	private void validatePositiveId(Long id) {
		if (id == null || id <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
