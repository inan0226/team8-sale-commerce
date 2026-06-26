package com.example.team8salecommerce.domain.search.dto;

import java.util.List;

public record ProductSearchResponse(
    List<SearchProductDetailResponse> content,
    int page,
    int size,
    int totalPages
) {
}
