package com.example.team8salecommerce.domain.refund.entity;

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

@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long orderId;

	@Column(nullable = false)
	private Long paymentId;

	@Column(nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private RefundReasonType refundReasonType;

	@Column(columnDefinition = "TEXT")
	private String reasonDetail;

	@Column(nullable = false)
	private Long refundAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private RefundStatus status;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	private LocalDateTime completedAt;

	private LocalDateTime failedAt;

	@Column(length = 255)
	private String failureReason;

	/**
	 * PortOne 결제 취소 ID
	 *
	 * PortOne 환불 API가 성공한 뒤 반환하는 취소 식별자이다.
	 * 내부 DB 완료 처리 중 문제가 생겨도 운영자가 실제 환불 성공 건을 추적할 수 있게 저장한다.
	 */
	@Column(length = 100)
	private String portOneCancellationId;

	/**
	 * PortOne 결제 취소 상태
	 *
	 * 예: SUCCEEDED
	 */
	@Column(length = 50)
	private String portOneCancellationStatus;

	/**
	 * PortOne 환불 성공 정보 저장 시간
	 *
	 * 실제 PortOne 환불 성공 이후 내부 완료 처리 전에 기록한다.
	 */
	private LocalDateTime portOneCancelledAt;

	private Refund(
		Long orderId,
		Long paymentId,
		Long memberId,
		RefundReasonType reasonType,
		String reasonDetail,
		Long refundAmount,
		LocalDateTime requestedAt
	) {
		this.orderId = orderId;
		this.paymentId = paymentId;
		this.memberId = memberId;
		this.refundReasonType = reasonType;
		this.reasonDetail = reasonDetail;
		this.refundAmount = refundAmount;
		this.status = RefundStatus.REFUND_REQUEST;
		this.requestedAt = requestedAt;
	}

	/**
	 * 환불 요청 정보를 생성한다.
	 *
	 * 환불 API에서 주문자 본인 확인, 결제 완료 상태 확인,
	 * 중복 환불 방지 검증이 끝난 뒤 호출한다.
	 */
	public static Refund createRequest(
		Long orderId,
		Long paymentId,
		Long memberId,
		RefundReasonType reasonType,
		String reasonDetail,
		Long refundAmount,
		LocalDateTime requestedAt
	) {
		return new Refund(
			orderId,
			paymentId,
			memberId,
			reasonType,
			reasonDetail,
			refundAmount,
			requestedAt
		);
	}

	/**
	 * PortOne 환불 성공 정보를 저장한다.
	 *
	 * PortOne 환불 API는 성공했지만,
	 * 내부 DB 완료 처리 중 문제가 생길 수 있으므로
	 * 환불 완료 처리 전에 PortOne 성공 정보를 별도 상태로 먼저 남긴다.
	 */
	public void recordPortOneRefundSuccess(
		String cancellationId,
		String cancellationStatus,
		LocalDateTime portOneCancelledAt
	) {
		if (!isRequested()) {
			throw new CustomException(ErrorCode.REFUND_NOT_ALLOWED);
		}

		if (
			cancellationId == null
				|| cancellationId.isBlank()
				|| cancellationStatus == null
				|| cancellationStatus.isBlank()
				|| portOneCancelledAt == null
		) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		this.portOneCancellationId = cancellationId;
		this.portOneCancellationStatus = cancellationStatus;
		this.portOneCancelledAt = portOneCancelledAt;
		this.status = RefundStatus.PORTONE_REFUND_SUCCEEDED;
	}

	/**
	 * 환불을 완료 상태로 변경한다.
	 *
	 * PG 환불 요청이 성공한 뒤 호출한다.
	 * 환불 완료 후에는 주문 상태 변경과 이벤트 재고 복구가 이어진다.
	 */
	public void complete(LocalDateTime completedAt) {
		this.status = RefundStatus.REFUNDED;
		this.completedAt = completedAt;
		this.failedAt = null;
		this.failureReason = null;
	}

	/**
	 * 환불을 실패 상태로 변경한다.
	 *
	 * PG 환불 요청이 실패하거나 내부 처리 중 문제가 생겼을 때 호출한다.
	 */
	public void fail(LocalDateTime failedAt, String failureReason) {
		this.status = RefundStatus.REFUND_FAILED;
		this.failedAt = failedAt;
		this.failureReason = failureReason;
	}

	public boolean isRefunded() {
		return this.status == RefundStatus.REFUNDED;
	}

	/**
	 * 환불 요청 상태인지 확인한다.
	 *
	 * 아직 완료/실패 처리되지 않은 환불인지 판단할 때 사용할 수 있다.
	 */
	public boolean isRequested() {
		return this.status == RefundStatus.REFUND_REQUEST;
	}

	/**
	 * PortOne 환불은 성공했고 내부 완료 처리를 기다리는 상태인지 확인한다.
	 */
	public boolean isPortOneRefundSucceeded() {
		return this.status == RefundStatus.PORTONE_REFUND_SUCCEEDED;
	}
}
