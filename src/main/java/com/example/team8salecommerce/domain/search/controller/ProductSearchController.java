package com.example.team8salecommerce.domain.search.controller;

import com.example.team8salecommerce.domain.search.dto.ProductSearchResponse;
import com.example.team8salecommerce.domain.search.service.ProductSearchService;
import com.example.team8salecommerce.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping("/search/products")
    public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ProductSearchResponse response = productSearchService.searchProducts(
                keyword, categoryId, minPrice, maxPrice, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("상품 검색 성공", response));
    }
}
