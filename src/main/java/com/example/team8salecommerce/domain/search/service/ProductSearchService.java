package com.example.team8salecommerce.domain.search.service;

import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.search.dto.ProductSearchCache;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;

    @Cacheable(
            value = "productSearch",
            key = "#keyword + ':' + #minPrice + ':' + #maxPrice + ':' + #categoryId + ':' + #page + ':' + #size"
    )
    @Transactional(readOnly = true)
    public ProductSearchCache searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.searchProducts(keyword, categoryId, minPrice, maxPrice, pageable);
        return ProductSearchCache.from(productPage);
    }
}
