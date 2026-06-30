package com.example.team8salecommerce.domain.product.controller;

import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.service.ProductService;
import com.example.team8salecommerce.global.response.ApiResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ProductPageResponse response = productService.getProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("상품 목록 조회 성공", response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @PathVariable @Min(1) Long productId
    ) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success("상품 상세 조회 성공", response));
    }
}