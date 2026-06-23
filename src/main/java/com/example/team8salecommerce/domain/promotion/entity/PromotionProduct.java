package com.example.team8salecommerce.domain.promotion.entity;

import java.time.LocalDateTime;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특가 상품 엔티티
 *
 * 일반 상품 중에서 선착순 이벤트로 판매되는 상품 정보를 관리한다.
 * 선착순 이벤트에서는 일반 상품 재고가 아니라 remainingEventStock을 차감한다.
 */
@Getter
@Entity
@Table(name = "promotion_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false)
	private Long promotionPrice;

	@Column(nullable = false)
	private Integer discountRate;

	@Column(nullable = false)
	private Integer totalEventStock;

	@Column(nullable = false)
	private Integer remainingEventStock;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PromotionProductStatus status;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;

	private PromotionProduct(
		Long productId,
		String title,
		Long promotionPrice,
		Integer discountRate,
		Integer totalEventStock,
		LocalDateTime startTime,
		LocalDateTime endTime
	) {
		this.productId = productId;
		this.title = title;
		this.promotionPrice = promotionPrice;
		this.discountRate = discountRate;
		this.totalEventStock = totalEventStock;

		// 처음 생성될 때는 남은 재고가 최초 재고와 동일하다.
		this.remainingEventStock = totalEventStock;

		// 처음 생성된 특가 상품은 아직 판매 전 상태로 둔다.
		this.status = PromotionProductStatus.READY;

		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * 특가 상품 생성 팩토리 메서드
	 *
	 * 서비스 계층에서는 new PromotionProduct(...) 대신
	 * PromotionProduct.create(...) 형태로 객체를 생성한다.
	 */
	public static PromotionProduct create(
		Long productId,
		String title,
		Long promotionPrice,
		Integer discountRate,
		Integer totalEventStock,
		LocalDateTime startTime,
		LocalDateTime endTime
	) {
		return new PromotionProduct(
			productId,
			title,
			promotionPrice,
			discountRate,
			totalEventStock,
			startTime,
			endTime
		);
	}

	public void open() {
		this.status = PromotionProductStatus.OPEN;
	}

	public void close() {
		this.status = PromotionProductStatus.CLOSED;
	}

	/**
	 * 구매 가능 여부를 검증한다.
	 *
	 * 선착순 구매 요청 시 재고 차감 전에 호출한다.
	 * 이벤트가 진행 중인지, 재고가 충분한지 확인한다.
	 */
	public void validatePurchaseable(LocalDateTime now, Integer quantity) {
		validateQuantity(quantity);

		if (!isOpen(now)) {
			throw new CustomException(ErrorCode.PROMOTION_NOT_OPEN);
		}

		if (remainingEventStock < quantity) {
			throw new CustomException(ErrorCode.OUT_OF_STOCK);
		}
	}

	public void decreaseStock(Integer quantity) {
		validateQuantity(quantity);

		if (remainingEventStock < quantity) {
			throw new CustomException(ErrorCode.OUT_OF_STOCK);
		}

		this.remainingEventStock -= quantity;

		if (this.remainingEventStock == 0) {
			this.status = PromotionProductStatus.SOLD_OUT;
		}
	}

	/**
	 * 이벤트 재고를 복구한다.
	 *
	 * 결제 실패 또는 환불 성공 시 호출된다.
	 * 복구된 재고가 최초 이벤트 재고를 넘지 않도록 제한한다.
	 */
	public void restoreStock(Integer quantity, LocalDateTime now) {
		validateQuantity(quantity);

		int restoredStock = this.remainingEventStock +  quantity;

		// 복구 후 재고가 최초 재고를 초과히지 않도록 막는다.
		this.remainingEventStock = Math.min(restoredStock, this.remainingEventStock);

		// 품절 상태였지만 재고가 복구되고 이벤트 시간이 아직 유효하면 다시 구매 가능 상태로 변경한다.
		if (this.status == PromotionProductStatus.SOLD_OUT && isWithinEventTime(now)) {
			this.status = PromotionProductStatus.OPEN;
		}
	}

	/**
	 * 현재 특가 상품이 구매 가능한 상태인지 확인한다.
	 *
	 * 상태가 OPEN이어야 하고,
	 * 현재 시간이 이벤트 시작 시간과 종료 시간 사이여야 한다.
	 */
	private boolean isOpen(LocalDateTime now) {
		return this.status == PromotionProductStatus.OPEN && isWithinEventTime(now);
	}


	/**
	 * 현재 시간이 이벤트 진행 시간 안에 있는지 확인한다.
	 *
	 * startTime <= now < endTime 기준이다.
	 */
	private boolean isWithinEventTime(LocalDateTime now) {
		return !now.isBefore(this.startTime) && now.isBefore(this.endTime);
	}

	/**
	 * 구매 또는 복구 수량이 올바른지 검증한다.
	 *
	 * 수량은 null이면 안 되고, 1개 이상이어야 한다.
	 */
	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
