package com.example.team8salecommerce.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductPageResponse {

    private List<ProductListResponse> content;
}
