package com.example.team8salecommerce.domain.auth.dto;

public record LoginResponse(
        Long memberId,
        String email,
        String nickname,
        String role,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
