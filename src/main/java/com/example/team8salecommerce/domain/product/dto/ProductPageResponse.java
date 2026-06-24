package com.example.team8salecommerce.domain.product.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductListResponse> content,
        int page,
        int size,
        int totalPages,
        long totalElements
) {
}
