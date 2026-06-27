package com.example.team8salecommerce.domain.search.controller;

import com.example.team8salecommerce.domain.product.exception.ProductException;
import com.example.team8salecommerce.domain.search.dto.ProductSearchResponse;
import com.example.team8salecommerce.domain.search.service.ProductSearchService;
import com.example.team8salecommerce.domain.search.service.SearchKeywordService;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final SearchKeywordService searchKeywordService;

    @GetMapping("/products/search")
    public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @Min(0) Long minPrice,
            @RequestParam(required = false) @Min(0) Long maxPrice,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        if ((keyword == null || keyword.trim().isEmpty()) && categoryId == null && minPrice == null && maxPrice == null) {
            throw new ProductException(ErrorCode.INVALID_SEARCH_CONDITION);
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ProductException(ErrorCode.INVALID_PRICE_RANGE);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeywordService.incrementKeywordCount(keyword);
        }

        ProductSearchResponse response = productSearchService.searchProducts(
                keyword, categoryId, minPrice, maxPrice, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("상품 검색 성공", response));
    }
}
