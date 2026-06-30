package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.exception.ProductException;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(Pageable pageable) {
        log.info("상품 목록 조회 시작");
        Page<Product> productPage = productRepository.findByIsDeletedFalse(pageable);
        log.info("상품 목록 조회 완료");
        return ProductPageResponse.from(productPage);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsDeletedFalse(categoryId, pageable);
    }
}
