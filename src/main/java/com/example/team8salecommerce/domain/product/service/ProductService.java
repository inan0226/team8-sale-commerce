package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.dto.ProductListResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(
            int page,
            int size,
            String sort
    ) {

        Sort sortOption = switch (sort) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            default -> Sort.by("id").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);

        Page<ProductListResponse> result =
                productRepository.findAll(pageable)
                        .map(ProductListResponse::from);

        return new ProductPageResponse(result.getContent());
    }
}
