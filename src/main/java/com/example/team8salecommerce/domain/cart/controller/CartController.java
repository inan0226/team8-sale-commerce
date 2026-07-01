package com.example.team8salecommerce.domain.cart.controller;

import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.request.UpdateCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.response.CartItemResponse;
import com.example.team8salecommerce.domain.cart.dto.response.CartResponse;
import com.example.team8salecommerce.domain.cart.service.CartService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

	private final CartService cartService;
	private final AuthMemberResolver authMemberResolver;

	// 장바구니 상품 추가
	@PostMapping("/items")
	public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
		@AuthenticationPrincipal AuthMember authMember,
		@Valid @RequestBody AddCartItemRequest request
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);
		CartItemResponse response = cartService.addCartItem(memberId, request);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// 장바구니 조회
	@GetMapping
	public ResponseEntity<ApiResponse<CartResponse>> getCart(
		@AuthenticationPrincipal AuthMember authMember
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);
		CartResponse response = cartService.getCart(memberId);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// 장바구니 상품 수량 변경
	@PatchMapping("/items/{cartItemId}")
	public ResponseEntity<ApiResponse<CartItemResponse>>
	updateCartItemQuantity(
		@AuthenticationPrincipal AuthMember authMember,
		@PathVariable Long cartItemId,
		@Valid @RequestBody UpdateCartItemRequest request
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);
		CartItemResponse response = cartService.updateCartItemQuantity(
			memberId,
			cartItemId,
			request
		);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// 장바구니 상품 삭제
	@DeleteMapping("/items/{cartItemId}")
	public ResponseEntity<ApiResponse<Void>>
	deleteCartItem(
		@AuthenticationPrincipal AuthMember authMember,
		@PathVariable Long cartItemId
	) {
		Long memberId = authMemberResolver.requireMemberId(authMember);
		cartService.deleteCartItem(memberId, cartItemId);

		return ResponseEntity.ok(ApiResponse.success(null));
	}
}
