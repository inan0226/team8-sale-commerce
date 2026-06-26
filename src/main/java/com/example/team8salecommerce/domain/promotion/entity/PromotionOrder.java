package com.example.team8salecommerce.domain.promotion.entity;

import java.time.LocalDateTime;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.util.BaseEntity;

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
 * 특가 주문 엔티티
 *
 * 선착순 특가 상품 구매 시 생성되는 주문이다.
 * 일반 장바구니 주문과 분리하여, 특가 구매/결제/환불 흐름을 독립적으로 관리한다.
 */
@Getter
@Entity
@Table(name = "promotion_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionOrder extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 주문한 회원 ID
	 */
	@Column(nullable = false)
	private Long memberId;

	/**
	 * 구매한 특가 상품 ID
	 */
	@Column(nullable = false)
	private Long promotionProductId;

	/**
	 * 주문 총 금액
	 */
	@Column(nullable = false)
	private Long totalAmount;

	/**
	 * 특가 주문 상태
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PromotionOrderStatus status;

	/**
	 * 주문 생성 시간
	 */
	@Column(nullable = false)
	private LocalDateTime orderedAt;

	/**
	 * 결제 완료 시간
	 */
	private LocalDateTime paidAt;

	/**
	 * 결제 실패 시간
	 */
	private LocalDateTime paymentFailedAt;

	/**
	 * 환불 요청 시간
	 */
	private LocalDateTime refundRequestedAt;

	/**
	 * 환불 완료 시간
	 */
	private LocalDateTime refundedAt;

	private PromotionOrder(
		Long memberId,
		Long promotionProductId,
		Long totalAmount,
		LocalDateTime orderedAt
	) {
		validateCreate(memberId, promotionProductId, totalAmount, orderedAt);

		this.memberId = memberId;
		this.promotionProductId = promotionProductId;
		this.totalAmount = totalAmount;
		this.status = PromotionOrderStatus.WAITING;
		this.orderedAt = orderedAt;
	}

	/**
	 * 특가 주문을 생성한다.
	 *
	 * 선착순 구매 성공 후 결제 대기 상태의 주문을 만든다.
	 */
	public static PromotionOrder create(
		Long memberId,
		Long promotionProductId,
		Long totalAmount,
		LocalDateTime orderedAt
	) {
		return new PromotionOrder(memberId, promotionProductId, totalAmount, orderedAt);
	}

	/**
	 * 주문을 결제 완료 상태로 변경한다.
	 *
	 * 결제 대기 상태인 주문만 결제 완료로 변경할 수 있다.
	 */
	public void markAsPaid(LocalDateTime paidAt) {
		if (!isWaiting()) {
			throw new CustomException(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);
		}

		if (paidAt == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		this.status = PromotionOrderStatus.PAID;
		this.paidAt = paidAt;
	}

	/**
	 * 주문을 결제 실패 상태로 변경한다.
	 *
	 * 결제 실패 시 선점했던 이벤트 재고는 별도 서비스에서 복구한다.
	 */
	public void failPayment(LocalDateTime paymentFailedAt) {
		if (!isWaiting()) {
			throw new CustomException(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);
		}

		if (paymentFailedAt == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		this.status = PromotionOrderStatus.PAYMENT_FAILED;
		this.paymentFailedAt = paymentFailedAt;
	}

	/**
	 * 주문을 환불 요청 상태로 변경한다.
	 *
	 * 결제 완료 상태인 주문만 환불 요청할 수 있다.
	 */
	public void requestRefund(LocalDateTime refundRequestedAt) {
		if (!isPaid()) {
			throw new CustomException(ErrorCode.REFUND_NOT_ALLOWED);
		}

		if (refundRequestedAt == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		this.status = PromotionOrderStatus.REFUND_REQUEST;
		this.refundRequestedAt = refundRequestedAt;
	}

	/**
	 * 주문을 환불 완료 상태로 변경한다.
	 *
	 * 환불 요청 상태인 주문만 환불 완료로 변경할 수 있다.
	 */
	public void completeRefund(LocalDateTime refundedAt) {
		if (!isRefundRequested()) {
			throw new CustomException(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);
		}

		if (refundedAt == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		this.status = PromotionOrderStatus.REFUNDED;
		this.refundedAt = refundedAt;
	}

	/**
	 * 환불 요청 실패 후 주문을 다시 결제 완료 상태로 되돌린다.
	 *
	 * PortOne 환불 요청이 실패한 경우,
	 * 실제 결제 취소가 되지 않았으므로 주문 상태를 PAID로 복구한다.
	 */
	public void failRefundRequest() {
		if (!isRefundRequested()) {
			throw new CustomException(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);
		}

		this.status = PromotionOrderStatus.PAID;
		this.refundRequestedAt = null;
	}

	/**
	 * 결제 대기 상태인지 확인한다.
	 */
	public boolean isWaiting() {
		return status == PromotionOrderStatus.WAITING;
	}

	/**
	 * 결제 완료 상태인지 확인한다.
	 */
	public boolean isPaid() {
		return status == PromotionOrderStatus.PAID;
	}

	/**
	 * 환불 요청 상태인지 확인한다.
	 */
	public boolean isRefundRequested() {
		return status == PromotionOrderStatus.REFUND_REQUEST;
	}

	/**
	 * 환불 완료 상태인지 확인한다.
	 */
	public boolean isRefunded() {
		return status == PromotionOrderStatus.REFUNDED;
	}

	/**
	 * 주문 생성 시 필수값을 검증한다.
	 */
	private void validateCreate(
		Long memberId,
		Long promotionProductId,
		Long totalAmount,
		LocalDateTime orderedAt
	) {
		if (memberId == null || promotionProductId == null || totalAmount == null || orderedAt == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (totalAmount <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
