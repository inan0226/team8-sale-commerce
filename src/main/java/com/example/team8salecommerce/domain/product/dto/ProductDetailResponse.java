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
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory().getName(),
                product.getViewCount()
        );
    }
}