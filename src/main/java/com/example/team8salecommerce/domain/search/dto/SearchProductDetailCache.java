package com.example.team8salecommerce.domain.search.dto;

import com.example.team8salecommerce.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchProductDetailCache {
    private Long id;
    private String name;
    private Long price;

    public static SearchProductDetailCache from(Product product) {
        return new SearchProductDetailCache(
                product.getId(),
                product.getName(),
                product.getPrice()
        );
    }
}
