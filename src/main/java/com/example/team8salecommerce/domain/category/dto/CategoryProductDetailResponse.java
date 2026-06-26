package com.example.team8salecommerce.domain.category.dto;

import com.example.team8salecommerce.domain.product.entity.Product;

public record CategoryProductDetailResponse(
    Long id,
    String name,
    Long price
) {
    public static CategoryProductDetailResponse from(Product product) {
        return new CategoryProductDetailResponse(
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }
}
