package com.example.team8salecommerce.domain.product.dto;

import com.example.team8salecommerce.domain.product.entity.Product;

public record ProductListResponse(
        Long id,
        String name,
        String brand,
        Long price,
        Integer stock,
        String imageUrl
) {

    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl()
        );
    }
}