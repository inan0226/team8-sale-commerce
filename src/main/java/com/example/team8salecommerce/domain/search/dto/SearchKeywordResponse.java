package com.example.team8salecommerce.domain.search.dto;

public record SearchKeywordResponse(
    int rank,
    String keyword,
    long count
) {
}
