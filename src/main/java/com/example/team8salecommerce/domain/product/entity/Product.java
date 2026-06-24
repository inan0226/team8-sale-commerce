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
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
