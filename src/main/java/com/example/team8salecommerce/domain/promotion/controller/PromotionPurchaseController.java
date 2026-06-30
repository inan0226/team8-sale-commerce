package com.example.team8salecommerce.domain.promotion.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseRequest;
import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseResponse;
import com.example.team8salecommerce.domain.promotion.facade.PromotionPurchaseFacade;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 선착순 특가 구매 Controller
 *
 * 로그인한 사용자의 선착순 특가 상품 구매 요청을 처리한다.
 *
 * API:
 * POST /promotions/{promotionProductId}/purchase
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/promotions")
public class PromotionPurchaseController {

	private final PromotionPurchaseFacade promotionPurchaseFacade;
	private final AuthMemberResolver authMemberResolver;

	/**
	 * 선착순 특가 상품을 구매한다.
	 *
	 * 인증된 회원 ID와 특가 상품 ID, 구매 수량을 기반으로
	 * Redis Lock을 사용한 선착순 구매 흐름을 실행한다.
	 */
	@PostMapping("/{promotionProductId}/purchase")
	public ResponseEntity<ApiResponse<PromotionPurchaseResponse>> purchase(
		@AuthenticationPrincipal AuthMember authMember,
		@PathVariable Long promotionProductId,
		@Valid @RequestBody PromotionPurchaseRequest request
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);

		PromotionPurchaseResponse responseDto = promotionPurchaseFacade.purchase(
			memberId,
			promotionProductId,
			request
		);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success(responseDto));
	}
}
