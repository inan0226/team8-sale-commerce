package com.example.team8salecommerce.domain.product.repository;

import com.example.team8salecommerce.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    Page<Product> searchProducts(String keyword, Long categoryId, Long minPrice, Long maxPrice, Pageable pageable);
}
