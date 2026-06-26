package com.example.team8salecommerce.domain.product.controller;

import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.enumtype.ProductSortType;
import com.example.team8salecommerce.domain.product.service.ProductService;
import com.example.team8salecommerce.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts(

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(defaultValue = "LATEST")
            ProductSortType sort
    ) {

        ProductPageResponse response =
                productService.getProducts(page, size, sort);

        return ResponseEntity.ok(
                ApiResponse.success("상품 목록 조회 성공", response)
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @PathVariable @Min(1) Long productId
    ) {

        ProductDetailResponse response =
                productService.getProductDetail(productId);

        return ResponseEntity.ok(
                ApiResponse.success("상품 상세 조회 성공", response)
        );
    }
}