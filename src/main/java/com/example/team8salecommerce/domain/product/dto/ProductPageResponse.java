package com.example.team8salecommerce.domain.product.dto;

import com.example.team8salecommerce.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import java.util.List;

public record ProductPageResponse(
        List<ProductListResponse> content,
        int page,
        int size,
        int totalPages,
        long totalElements
) {
    public static ProductPageResponse from(Page<Product> productPage) {
        List<ProductListResponse> content = productPage.stream()
                .map(ProductListResponse::from)
                .toList();

        return new ProductPageResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }
}
