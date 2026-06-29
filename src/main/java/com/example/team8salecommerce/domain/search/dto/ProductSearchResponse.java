package com.example.team8salecommerce.domain.search.dto;

import com.example.team8salecommerce.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import java.util.List;

public record ProductSearchResponse(
    List<SearchProductDetailResponse> content,
    int page,
    int size,
    int totalPages,
    long totalElements
) {
    public static ProductSearchResponse from(Page<Product> productPage) {
        List<SearchProductDetailResponse> content = productPage.stream()
                .map(SearchProductDetailResponse::from)
                .toList();

        return new ProductSearchResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }
}
