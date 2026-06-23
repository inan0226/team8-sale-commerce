package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.enumtype.ProductSortType;
import com.example.team8salecommerce.domain.product.dto.ProductListResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
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
            default -> org.springframework.data.domain.Sort.by("id").descending(); // LATEST
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);

        Page<Product> productPage = productRepository.findAll(pageable);

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
}
