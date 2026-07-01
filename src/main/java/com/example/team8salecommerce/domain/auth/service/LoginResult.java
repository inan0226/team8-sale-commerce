package com.example.team8salecommerce.domain.auth.service;

import com.example.team8salecommerce.domain.auth.dto.LoginResponse;

public record LoginResult(
        LoginResponse response,
        String refreshToken,
        long refreshTokenMaxAgeMillis
) {
}
