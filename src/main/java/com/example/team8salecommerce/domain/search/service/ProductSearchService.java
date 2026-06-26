package com.example.team8salecommerce.domain.search.service;

import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.search.dto.ProductSearchResponse;
import com.example.team8salecommerce.domain.search.dto.SearchProductDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;

    @Cacheable(
            value = "productSearch",
            key = "#keyword + ':' + #minPrice + ':' + #maxPrice + ':' + #categoryId"
    )
    @Transactional(readOnly = true)
    public ProductSearchResponse searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Product> spec = ProductSearchSpecification.searchProducts(keyword, categoryId, minPrice, maxPrice);

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<SearchProductDetailResponse> content = productPage.stream()
                .map(SearchProductDetailResponse::from)
                .toList();

        return new ProductSearchResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages()
        );
    }
}
