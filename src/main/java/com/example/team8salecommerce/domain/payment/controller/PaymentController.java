package com.example.team8salecommerce.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.facade.PaymentFailFacade;
import com.example.team8salecommerce.domain.payment.service.PaymentService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 결제 Controller
 *
 * 결제 승인, 결제 실패 처리 등
 * 결제 관련 API 요청을 받는 역할을 담당한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

	private final PaymentService paymentService;
	private final PaymentFailFacade paymentFailFacade;
	private final AuthMemberResolver authMemberResolver;

	/**
	 * 결제 승인 API
	 *
	 * 사용자가 PortOne 결제를 완료한 뒤,
	 * 클라이언트가 서버에 결제 승인을 요청할 때 호출한다.
	 */
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirmPayment(
		@AuthenticationPrincipal AuthMember authMember,
		@Valid @RequestBody PaymentConfirmRequest request
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);

		PaymentConfirmResponse responseDto = paymentService.confirmPayment(
			memberId,
			request
		);

		return ResponseEntity.ok(ApiResponse.success(responseDto));
	}

	/**
	 * 결제 실패 처리 API
	 *
	 * PortOne 결제 실패 후 클라이언트가 서버에 실패 처리를 요청할 때 호출한다.
	 *
	 * Controller는 HTTP 요청/응답만 담당하고,
	 * 실제 결제 실패 처리 흐름은 PaymentFailFacade에 위임한다.
	 */
	@PostMapping("/fail")
	public ResponseEntity<ApiResponse<PaymentFailResponse>> failPayment(
		@AuthenticationPrincipal AuthMember authMember,
		@Valid @RequestBody PaymentFailRequest request
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);

		PaymentFailResponse responseDto = paymentFailFacade.failPayment(
			memberId,
			request
		);

		return ResponseEntity.ok(ApiResponse.success(responseDto));
	}
}
