package com.example.team8salecommerce.domain.auth.dto;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType
) {
}
