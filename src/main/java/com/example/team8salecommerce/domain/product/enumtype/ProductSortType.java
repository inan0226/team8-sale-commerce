package com.example.team8salecommerce.domain.product.enumtype;

public enum ProductSortType {
    LATEST,
    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC,
    NAME_DESC;

    public static ProductSortType from(String value) {
        return ProductSortType.valueOf(value.toUpperCase());
    }
}