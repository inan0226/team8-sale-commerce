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
        String description = product.getDescription() != null ? product.getDescription() : "상품 설명 없음";
        Integer viewCount = product.getViewCount() != null ? product.getViewCount() : 0;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "UNKNOWN";

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                description,
                product.getPrice(),
                product.getStock(),
                categoryName,
                viewCount
        );
    }
}