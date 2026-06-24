package com.example.team8salecommerce.domain.product.dto;

public record ProductDetailResponse(
        Long id,
        String name,
        String brand,
        String description,
        Long price,
        Integer stock,
        String category,
        Integer viewCount
) {
}