package com.example.team8salecommerce.domain.search.dto;

import com.example.team8salecommerce.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchCache {
    private List<SearchProductDetailCache> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public static ProductSearchCache from(Page<Product> productPage) {
        List<SearchProductDetailCache> content = productPage.stream()
                .map(SearchProductDetailCache::from)
                .toList();

        return new ProductSearchCache(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }
}
