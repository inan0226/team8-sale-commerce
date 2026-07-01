package com.example.team8salecommerce.domain.auth.service;

import com.example.team8salecommerce.domain.auth.dto.TokenRefreshResponse;

public record TokenRefreshResult(
        TokenRefreshResponse response,
        String refreshToken,
        long refreshTokenMaxAgeMillis
) {
}
