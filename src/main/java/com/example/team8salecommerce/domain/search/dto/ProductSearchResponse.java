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
public class ProductSearchResponse {
    private List<SearchProductDetailResponse> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public static ProductSearchResponse from(Page<Product> productPage) {
        List<SearchProductDetailResponse> content = productPage.stream()
                .map(SearchProductDetailResponse::from)
                .toList();

        return new ProductSearchResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements()
        );
    }
}
