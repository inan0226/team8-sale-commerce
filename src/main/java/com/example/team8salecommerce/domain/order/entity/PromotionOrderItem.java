package com.example.team8salecommerce.domain.order.entity;

import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


// 특가 주문 시점의 원가, 특가, 할인율과 수량을 보존하는 엔티티

@Getter
@Entity(name = "CommonPromotionOrderItem")
@Table(name = "promotion_order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionOrderItem extends BaseEntity {

    // 특가 주문 상품 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 주문 대상 특가 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_product_id", nullable = false)
    private PromotionProduct promotionProduct;

    // 특가 상품이 가르키는 원본 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 주문 시점 상품 스냅샷
    @Column(nullable = false)
    private String productName;

    // 할인 전 상품 가격
    @Column(nullable = false)
    private Long originalPrice;

    // 주문 시점 특가 가격
    @Column(nullable = false)
    private Long promotionPrice;

    // 주문 시점 할인률
    @Column(nullable = false)
    private Integer discountRate;

    // 특가 주문 수량
    @Column(nullable = false)
    private Integer quantity;

    private PromotionOrderItem(
            Order order,
            PromotionProduct promotionProduct,
            Product product,
            Integer quantity
    ) {
        validateCreate(order, promotionProduct, product, quantity);
        this.order = order;
        this.promotionProduct = promotionProduct;
        this.product = product;
        this.productName = product.getName();
        this.originalPrice = product.getPrice();
        this.promotionPrice = promotionProduct.getPromotionPrice();
        this.discountRate = promotionProduct.getDiscountRate();
        this.quantity = quantity;
    }


    // 특가 상품 정보를 공통 주문 아래의 특가 스냅샷으로 생성
    public static PromotionOrderItem create(
            Order order,
            PromotionProduct promotionProduct,
            Product product,
            Integer quantity
    ) {
        return new PromotionOrderItem(order, promotionProduct, product, quantity);
    }

    public Long calculateTotalPrice() {
        return promotionPrice * quantity;
    }

    // 특가 주문 검증
    private void validateCreate(
            Order order,
            PromotionProduct promotionProduct,
            Product product,
            Integer quantity
    ) {
        if (order == null || promotionProduct == null || product == null || quantity == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (product.getName() == null || product.getName().isBlank()
                || product.getPrice() == null || product.getPrice() <= 0
                || promotionProduct.getPromotionPrice() == null
                || promotionProduct.getPromotionPrice() <= 0
                || promotionProduct.getDiscountRate() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_QUANTITY);
        }
    }
}