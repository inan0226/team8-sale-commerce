package com.example.team8salecommerce.domain.category.controller;

import com.example.team8salecommerce.domain.category.dto.CategoryProductResponse;
import com.example.team8salecommerce.domain.category.service.CategoryService;
import com.example.team8salecommerce.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories/{category}/products")
    public ResponseEntity<ApiResponse<CategoryProductResponse>> getCategoryProducts(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        CategoryProductResponse response = categoryService.getCategoryProducts(category, pageable);

        return ResponseEntity.ok(ApiResponse.success("카테고리별 상품 목록 조회 성공", response));
    }
}
