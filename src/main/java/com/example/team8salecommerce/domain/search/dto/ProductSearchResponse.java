package com.example.team8salecommerce.domain.search.dto;

import java.util.List;

public record ProductSearchResponse(
    List<SearchProductDetailResponse> content,
    int page,
    int size,
    int totalPages,
    long totalElements
) {
    public static ProductSearchResponse from(ProductSearchCache cache) {
        List<SearchProductDetailResponse> content = cache.getContent().stream()
                .map(detail -> new SearchProductDetailResponse(detail.getId(), detail.getName(), detail.getPrice()))
                .toList();

        return new ProductSearchResponse(
                content,
                cache.getPage(),
                cache.getSize(),
                cache.getTotalPages(),
                cache.getTotalElements()
        );
    }
}
