package com.example.team8salecommerce.domain.product.entity;

import com.example.team8salecommerce.global.util.BaseEntity;
import com.example.team8salecommerce.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "products")
public class Product extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String brand;

    private Long price;

    private Integer stock;

    private String imageUrl;

    @Column(nullable = false)
    private String description = "상품 설명 없음";

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private Product(
            String name,
            String brand,
            Long price,
            Integer stock,
            String imageUrl,
            String description,
            Boolean isDeleted,
            Integer viewCount,
            Category category
    ) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.description = description;
        this.isDeleted = isDeleted;
        this.viewCount = viewCount;
        this.category = category;
    }

    public static Product create(
            String name,
            String brand,
            Long price,
            Integer stock,
            String imageUrl,
            String description,
            Category category
    ) {
        return new Product(
                name,
                brand,
                price,
                stock,
                imageUrl,
                description,
                false,
                0,
                category
        );
    }

    public static Product createDeleted(
            String name,
            String brand,
            Long price,
            Integer stock,
            String imageUrl,
            String description,
            Category category
    ) {
        return new Product(
                name,
                brand,
                price,
                stock,
                imageUrl,
                description,
                true,
                0,
                category
        );
    }
}
