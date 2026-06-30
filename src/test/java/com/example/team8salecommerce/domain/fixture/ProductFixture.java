package com.example.team8salecommerce.domain.fixture;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.entity.Product;

public class ProductFixture {

    public static Product 상품(Category category) {
        return Product.create(
                "상품명",
                "브랜드",
                1000L,
                10,
                "img.jpg",
                "설명",
                category
        );
    }

    public static Product 삭제된상품(Category category) {
        return Product.createDeleted(
                "상품명2",
                "브랜드",
                1000L,
                10,
                "img.jpg",
                "설명",
                category
        );
    }

    public static ProductDetailResponse 상품상세응답(Product product) {
        return ProductDetailResponse.from(product);
    }
}