package com.example.team8salecommerce.domain.auth.controller;

import com.example.team8salecommerce.domain.auth.dto.LoginRequest;
import com.example.team8salecommerce.domain.auth.dto.LoginResponse;
import com.example.team8salecommerce.domain.auth.dto.SignupRequest;
import com.example.team8salecommerce.domain.auth.dto.SignupResponse;
import com.example.team8salecommerce.domain.auth.service.AuthService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.BearerTokenResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final BearerTokenResolver bearerTokenResolver;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", authService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        authService.logout(bearerTokenResolver.resolve(authorizationHeader));
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
}
