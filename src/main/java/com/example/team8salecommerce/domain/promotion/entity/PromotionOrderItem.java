package com.example.team8salecommerce.domain.promotion.entity;

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

	/**
	 * 특가 주문 ID
	 */
	@Column(nullable = false)
	private Long promotionOrderId;

	/**
	 * 특가 상품 ID
	 */
	@Column(nullable = false)
	private Long promotionProductId;

	/**
	 * 원본 상품 ID
	 */
	@Column(nullable = false)
	private Long productId;

	/**
	 * 주문 시점의 상품명
	 */
	@Column(nullable = false)
	private String productName;

	/**
	 * 구매 수량
	 */
	@Column(nullable = false)
	private Integer quantity;

	/**
	 * 주문 시점의 특가 단가
	 */
	@Column(nullable = false)
	private Long unitPrice;

	/**
	 * 총 주문 금액
	 *
	 * unitPrice * quantity
	 */
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
		this.totalPrice = calculateTotalPrice(unitPrice, quantity);
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

	/**
	 * 주문 상품 생성 시 필수값을 검증한다.
	 */
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

		if (quantity <= 0) {
			throw new CustomException(ErrorCode.INVALID_QUANTITY);
		}

		if (productName.isBlank() || unitPrice <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}

	/**
	 * 총 주문 금액을 계산한다.
	 */
	private Long calculateTotalPrice(Long unitPrice, Integer quantity) {
		return unitPrice * quantity;
	}
}
