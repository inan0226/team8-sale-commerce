package com.example.team8salecommerce.domain.search.dto;

import com.example.team8salecommerce.domain.product.entity.Product;

public record SearchProductDetailResponse(
    Long id,
    String name,
    Long price
) {
    public static SearchProductDetailResponse from(Product product) {
        return new SearchProductDetailResponse(
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }
}
