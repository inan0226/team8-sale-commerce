package com.example.team8salecommerce.domain.cart.controller;


import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.response.CartItemResponse;
import com.example.team8salecommerce.domain.cart.dto.response.CartResponse;
import com.example.team8salecommerce.domain.cart.service.CartService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    // 장바구니 상품 추가
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody AddCartItemRequest request
    ) {

        CartItemResponse response =
                cartService.addCartItem(
                        authMember.memberId(),
                        request
                );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        CartResponse response =
                cartService.getCart(authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
