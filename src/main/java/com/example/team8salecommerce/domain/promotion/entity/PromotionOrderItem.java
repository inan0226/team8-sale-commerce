package com.example.team8salecommerce.domain.promotion.entity;

import java.math.BigDecimal;

import org.springframework.data.domain.Auditable;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.util.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특가 주문 상품 엔티티
 *
 * 선착순 특가 주문에서 구매한 상품 정보를 저장한다.
 * 상품명, 가격 등은 주문 시점 기준으로 스냅샷처럼 저장한다.
 */
@Getter
@Entity
@Table(name = "promotion_order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionOrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long promotionOrderId;

	@Column(nullable = false)
	private Long promotionProductId;

	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false)
	private String productName;

	@Column(nullable = false)
	private Integer quantity;

	/**
	 * 주문 시점의 특가 단가
	 */
	@Column(nullable = false)
	private Long unitPrice;

	@Column(nullable = false)
	private Long totalPrice;

	private PromotionOrderItem(
		Long promotionOrderId,
		Long promotionProductId,
		Long productId,
		String productName,
		Integer quantity,
		Long unitPrice
	) {
		validateCreate(promotionOrderId, promotionProductId, productId, productName, quantity, unitPrice);

		this.promotionOrderId = promotionOrderId;
		this.promotionProductId = promotionProductId;
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.totalPrice = unitPrice * quantity;
	}

	/**
	 * 특가 주문 상품을 생성한다.
	 *
	 * 주문 생성 후 발급된 promotionOrderId를 기준으로 주문 상품을 저장한다.
	 */
	public static PromotionOrderItem create(
		Long promotionOrderId,
		Long promotionProductId,
		Long productId,
		String productName,
		Integer quantity,
		Long unitPrice
	) {
		return new PromotionOrderItem(
			promotionOrderId,
			promotionProductId,
			productId,
			productName,
			quantity,
			unitPrice
		);
	}

	private void validateCreate(
		Long promotionOrderId,
		Long promotionProductId,
		Long productId,
		String productName,
		Integer quantity,
		Long unitPrice
	) {
		if (
			promotionOrderId == null
			|| promotionProductId == null
			|| productId == null
			|| productName == null
			|| quantity == null
			|| unitPrice == null
		) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (productName.isBlank() || quantity <= 0 || unitPrice <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
