package com.example.team8salecommerce.domain.product.dto;

import com.example.team8salecommerce.domain.product.entity.Product;

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

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription() != null ? product.getDescription() : "상품 설명 없음",
                product.getPrice(),
                product.getStock(),
                product.getCategory() != null ? product.getCategory().getName() : "카테고리 없음",
                product.getViewCount() != null ? product.getViewCount() : 0
        );
    }
}