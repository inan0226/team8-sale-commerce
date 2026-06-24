package com.example.team8salecommerce.domain.stock.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
	* 재고 변경 이력 엔티티
 *
	 * 선착순 구매, 결제 실패, 환불 처리 과정에서 발생하는
 * 이벤트 재고 차감/복구 내역을 기록한다.
 *
	 * 재고 정합성 확인이나 테스트 결과 검증 시
 * "언제, 왜, 몇 개가, 얼마에서 얼마로 변경되었는지" 추적할 수 있다.
 */
@Getter
@Entity
@Table(name = "stock_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StockHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false)
	private Long promotionProductId;

	@Column(nullable = false)
	private Long orderId;

	/**
	 * 결제 ID
	 *
	 * 결제 실패로 재고가 복구되는 경우 어떤 결제와 관련된 복구인지 기록한다.
	 * 선착순 구매 차감 시점에는 결제 정보가 아직 없을 수 있으므로 nullable이다.
	 */
	private Long paymentId;

	/**
	 * 환불 ID
	 *
	 * 환불 완료로 재고가 복구되는 경우 어떤 환불과 관련된 복구인지 기록한다.
	 * 구매 차감이나 결제 실패 복구 시점에는 없을 수 있으므로 nullable이다.
	 */
	private Long refundId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StockChangeType type;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false)
	private Integer stockBefore;

	@Column(nullable = false)
	private Integer stockAfter;

	/**
	 * 재고 변경 사유
	 *
	 * PROMOTION_PURCHASE, PAYMENT_FAILED, REFUND_COMPLETED 중 하나로 저장한다.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private StockChangeReason reason;

	/**
	 * 외부에서 new로 직접 생성하지 못하게 private 생성자로 막는다.
	 *
	 * 객체 생성은 팀 컨벤션에 맞춰 정적 팩토리 메서드를 사용한다.
	 */
	private StockHistory(
		Long productId,
		Long promotionProductId,
		Long orderId,
		Long paymentId,
		Long refundId,
		StockChangeType type,
		Integer quantity,
		Integer stockBefore,
		Integer stockAfter,
		StockChangeReason reason
	) {
		this.productId = productId;
		this.promotionProductId = promotionProductId;
		this.orderId = orderId;
		this.paymentId = paymentId;
		this.refundId = refundId;
		this.type = type;
		this.quantity = quantity;
		this.stockBefore = stockBefore;
		this.stockAfter = stockAfter;
		this.reason = reason;
	}

	/**
	 * 선착순 구매로 재고가 차감된 이력을 생성한다.
	 *
	 * 결제 전 단계이므로 paymentId, refundId는 null로 저장한다.
	 */
	public static StockHistory createDecreaseHistory(
		Long productId,
		Long promotionProductId,
		Long orderId,
		Integer quantity,
		Integer stockBefore,
		Integer stockAfter
	) {

		validateStockChange(
			StockChangeType.DECREASE,
			quantity,
			stockBefore,
			stockAfter
		);

		return new StockHistory(
			productId,
			promotionProductId,
			orderId,
			null,
			null,
			StockChangeType.DECREASE,
			quantity,
			stockBefore,
			stockAfter,
			StockChangeReason.PROMOTION_PURCHASE
		);
	}

	/**
	 * 결제 실패로 재고가 복구된 이력을 생성한다.
	 *
	 * 결제 실패 처리에서는 paymentId를 함께 저장해서
	 * 어떤 결제 실패 때문에 복구되었는지 추적한다.
	 */
	public static StockHistory createPaymentFailRestoreHistory(
		Long productId,
		Long promotionProductId,
		Long orderId,
		Long paymentId,
		Integer quantity,
		Integer stockBefore,
		Integer stockAfter
	) {

		validateStockChange(
			StockChangeType.RESTORE,
			quantity,
			stockBefore,
			stockAfter
		);

		return new StockHistory(
			productId,
			promotionProductId,
			orderId,
			paymentId,
			null,
			StockChangeType.RESTORE,
			quantity,
			stockBefore,
			stockAfter,
			StockChangeReason.PAYMENT_FAILED
		);
	}

	/**
	 * 환불 완료로 재고가 복구된 이력을 생성한다.
	 *
	 * 환불 처리에서는 paymentId와 refundId를 함께 저장해서
	 * 어떤 결제/환불 때문에 복구되었는지 추적한다.
	 */
	public static StockHistory createRefundRestoreHistory(
		Long productId,
		Long promotionProductId,
		Long orderId,
		Long paymentId,
		Long refundId,
		Integer quantity,
		Integer stockBefore,
		Integer stockAfter
	) {

		validateStockChange(
			StockChangeType.RESTORE,
			quantity,
			stockBefore,
			stockAfter
		);

		return new StockHistory(
			productId,
			promotionProductId,
			orderId,
			paymentId,
			refundId,
			StockChangeType.RESTORE,
			quantity,
			stockBefore,
			stockAfter,
			StockChangeReason.REFUND_COMPLETED
		);
	}

	/**
	 * 재고 변경 이력의 수량 값을 검증한다.
	 *
	 * 재고 이력은 실제 재고 변경 결과를 기록하는 데이터이므로
	 * 잘못된 수량이나 계산 결과가 저장되지 않도록 검증한다.
	 */
	private static void validateStockChange(
		StockChangeType type,
		Integer quantity,
		Integer stockBefore,
		Integer stockAfter
	) {
		if (type == null || quantity == null || stockBefore == null || stockAfter == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (quantity <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (stockBefore < 0 || stockAfter < 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		validateStockCalculation(type, quantity, stockBefore, stockAfter);
	}

	/**
	 * 재고 변경 타입에 따라 변경 전/후 재고 계산이 맞는지 검증한다.
	 */
	private static void validateStockCalculation(
		StockChangeType type,
		Integer quantity,
		Integer stockBefore,
		Integer stockAfter
	) {
		if (type == StockChangeType.DECREASE) {
			int expectedStockAfter = stockBefore - quantity;

			if (!stockAfter.equals(expectedStockAfter)) {
				throw new CustomException(ErrorCode.INVALID_REQUEST);
			}

			return;
		}

		if (type == StockChangeType.RESTORE) {
			int expectedStockAfter = stockBefore + quantity;

			if (!stockAfter.equals(expectedStockAfter)) {
				throw new CustomException(ErrorCode.INVALID_REQUEST);
			}
		}
	}
}
