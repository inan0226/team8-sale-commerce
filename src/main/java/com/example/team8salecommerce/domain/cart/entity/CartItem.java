package com.example.team8salecommerce.domain.cart.entity;


import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.util.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 하나의 장바구니에 여러 상품이 담길 수 있음
// 동일 상품은 하나의 CartItem으로 관리, 수량만 증가하는 방식 사용
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "cart_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_product",
                        columnNames = {"cart_id", "product_id"}
                )
        }
)

public class CartItem extends BaseEntity {
    // 장바구니 상품 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 수량
    @Column(nullable = false)
    private int quantity;

    // 소속된 장바구니
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cart_id",
            nullable = false
    )
    private Cart cart;

    // 장바구니에 담긴 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;


    protected CartItem(
            Cart cart,
            Product product,
            int quantity
    ) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    // 장바구니 상품 생성
    public static CartItem create(
            Cart cart,
            Product product,
            int quantity
    ) {
        return new CartItem(
                cart,
                product,
                quantity
        );
    }

    // 수량 변경
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    // 동일 상품 추가 시 수량 증가
    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 장바구니 상품 삭제
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // 삭제된 장바구니 상품 복구
    public void restore(int quantity) {
        this.deletedAt = null;
        this.quantity = quantity;
    }

}
