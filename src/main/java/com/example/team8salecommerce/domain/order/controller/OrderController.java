package com.example.team8salecommerce.domain.order.controller;

import com.example.team8salecommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.team8salecommerce.domain.order.dto.response.OrderListResponse;
import com.example.team8salecommerce.domain.order.dto.response.OrderResponse;
import com.example.team8salecommerce.domain.order.service.OrderService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

   // 주문 생성
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        validateAuthenticatedMember(authMember);
        OrderResponse response = orderService.createOrder(authMember.memberId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문 생성 성공", response));
    }

    // 주문 조회
    @GetMapping
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        validateAuthenticatedMember(authMember);
        OrderListResponse response = orderService.getOrders(authMember.memberId());

        return ResponseEntity.ok(ApiResponse.success("주문 목록 조회 성공", response));
    }

    // 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable @Positive(message = "주문 ID는 양수여야 합니다.") Long orderId
    ) {
        validateAuthenticatedMember(authMember);
        OrderResponse response = orderService.cancelOrder(authMember.memberId(), orderId);

        return ResponseEntity.ok(ApiResponse.success("주문 취소 성공", response));
    }

    // 인증 회원 객체 검증
    private void validateAuthenticatedMember(AuthMember authMember) {
        if (authMember == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
