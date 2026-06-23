package com.example.team8salecommerce.domain.product.dto;

import com.example.team8salecommerce.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String brand;
    private Long price;
    private Integer stock;
    private String imageUrl;

    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl()
        );
    }
}
