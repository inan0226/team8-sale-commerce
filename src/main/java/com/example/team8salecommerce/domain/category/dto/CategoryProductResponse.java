package com.example.team8salecommerce.domain.category.dto;

import java.util.List;

public record CategoryProductResponse(
    List<CategoryProductDetailResponse> content,
    int page,
    int size,
    int totalPages,
    long totalElements
) {
}
