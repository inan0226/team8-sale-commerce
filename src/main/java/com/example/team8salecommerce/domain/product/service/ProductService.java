package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.enumtype.ProductSortType;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

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

        Pageable pageable = PageRequest.of(page, size, sortOption);

        System.out.println("Pageable 생성 완료");

        Page<Product> productPage =
                productRepository.findByIsDeletedFalse(pageable);

        System.out.println("상품 조회 완료");

        List<ProductListResponse> content = productPage
                .map(ProductListResponse::from)
                .getContent();

        return new ProductPageResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {

        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        String description =
                product.getDescription() != null ? product.getDescription() : "상품 설명 없음";

        Integer viewCount =
                product.getViewCount() != null ? product.getViewCount() : 0;

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                description,
                product.getPrice(),
                product.getStock(),
                product.getCategory() != null ? product.getCategory().getName() : "UNKNOWN",
                viewCount
        );
    }
}
