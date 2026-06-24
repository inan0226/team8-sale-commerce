package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.dto.ProductListResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.domain.product.exception.ProductException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(int page, int size, String sort) {

        Sort sortOption = createSort(sort);

        Pageable pageable = PageRequest.of(page, size, sortOption);

        Page<Product> productPage =
                productRepository.findByIsDeletedFalse(pageable);

        List<ProductListResponse> content =
                productPage.map(ProductListResponse::from).getContent();

        return new ProductPageResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }

    private Sort createSort(String sort) {

        if (sort == null) {
            return Sort.by("createdAt").descending();
        }

        return switch (sort) {
            case "price" -> Sort.by("price").descending();
            case "name" -> Sort.by("name").ascending();
            case "createdAt" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {

        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductDetailResponse.from(product);
    }
}
