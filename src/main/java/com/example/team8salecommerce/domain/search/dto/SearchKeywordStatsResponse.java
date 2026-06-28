package com.example.team8salecommerce.domain.search.dto;

public record SearchKeywordStatsResponse(
    String keyword,
    long count,
    String lastSearchedAt
) {
}
