package com.example.team8salecommerce.domain.category.service;

import com.example.team8salecommerce.domain.category.dto.CategoryProductDetailResponse;
import com.example.team8salecommerce.domain.category.dto.CategoryProductResponse;
import com.example.team8salecommerce.domain.category.exception.CategoryException;
import com.example.team8salecommerce.domain.category.repository.CategoryRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CategoryProductResponse getCategoryProducts(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Page<Product> productPage = productRepository.findByCategoryIdAndIsDeletedFalse(categoryId, pageable);
        List<CategoryProductDetailResponse> content = productPage.stream()
                .map(CategoryProductDetailResponse::from)
                .toList();

        return new CategoryProductResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }
}
