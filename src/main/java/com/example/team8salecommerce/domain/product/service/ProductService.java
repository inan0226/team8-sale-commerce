package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.enumtype.ProductSortType;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.domain.product.exception.ProductException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(int page, int size, ProductSortType sort) {

        var sortOption = switch (sort) {
            case PRICE_ASC -> org.springframework.data.domain.Sort.by("price").ascending();
            case PRICE_DESC -> org.springframework.data.domain.Sort.by("price").descending();
            case NAME_ASC -> org.springframework.data.domain.Sort.by("name").ascending();
            case NAME_DESC -> org.springframework.data.domain.Sort.by("name").descending();
            default -> org.springframework.data.domain.Sort.by("createdAt").descending();
        };

        log.info("상품 목록 조회 시작");

        Pageable pageable = PageRequest.of(page, size, sortOption);

        Page<Product> productPage =
                productRepository.findByIsDeletedFalse(pageable);

        log.info("상품 목록 조회 완료");

        return ProductPageResponse.from(productPage);
    }


    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {

        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductDetailResponse.from(product);
    }
}
