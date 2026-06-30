package com.example.team8salecommerce.domain.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.team8salecommerce.global.util.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "payments",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_payment_portone_payment_id",
			columnNames = "port_one_payment_id"
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long orderId;

	/** 일반 주문과 특가 주문의 같은 숫자 ID를 구분한다. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentOrderType orderType;

	/**
	 * PortOne 결제 ID
	 *
	 * 외부 결제 시스템에서 전달받는 결제 식별자이다.
	 * 중복 결제를 막기 위해 unique로 관리한다.
	 */
	@Column(name = "port_one_payment_id", nullable = false)
	private String portOnePaymentId;

	/**
	 * 결제 금액
	 *
	 * 주문 금액과 실제 결제 금액이 일치하는지 검증할 때 사용한다.
	 */
	@Column(nullable = false)
	private Long amount;

	/**
	 * 결제 수단
	 *
	 * 예: CARD, KAKAO_PAY, NAVER_PAY
	 * 현재 결제 승인 요청 DTO에는 포함하지 않지만,
	 * 추후 PortOne 응답에서 결제 수단을 저장할 수 있도록 nullable로 둔다.
	 */
	@Column(length = 50)
	private String method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentStatus status;

	private LocalDateTime paidAt;

	private LocalDateTime failedAt;

	@Column(length = 255)
	private String failureReason;

	/**
	 * 외부에서 new로 직접 생성하지 못하게 private 생성자로 막는다.
	 *
	 * 객체 생성은 팀 컨벤션에 따라 정적 팩토리 메서드를 사용한다.
	 */
	private Payment(
		Long orderId,
		PaymentOrderType orderType,
		String portOnePaymentId,
		Long amount,
		String method,
		PaymentStatus status,
		LocalDateTime paidAt,
		LocalDateTime failedAt,
		String failureReason
	) {
		this.orderId = orderId;
		this.orderType = orderType == null ? PaymentOrderType.PROMOTION : orderType;
		this.portOnePaymentId = portOnePaymentId;
		this.amount = amount;
		this.method = method;
		this.status = status;
		this.paidAt = paidAt;
		this.failedAt = failedAt;
		this.failureReason = failureReason;
	}

	/**
	 * 결제 승인 완료 결제 정보를 생성한다.
	 *
	 * 결제 승인 API에서 결제 금액 검증이 끝난 뒤 호출한다.
	 */
	public static Payment createPaidPayment(
		Long orderId,
		String portOnePaymentId,
		Long amount,
		String method,
		LocalDateTime paidAt
	) {
		return new Payment(
			orderId,
			PaymentOrderType.PROMOTION,
			portOnePaymentId,
			amount,
			method,
			PaymentStatus.PAID,
			paidAt,
			null,
			null
		);
	}


	/**
	 * 주문 유형을 명시해 결제 승인 정보를 생성한다.
	 */
	public static Payment createPaidPayment(
		Long orderId,
		PaymentOrderType orderType,
		String portOnePaymentId,
		Long amount,
		String method,
		LocalDateTime paidAt
	) {
		return new Payment(
			orderId,
			orderType,
			portOnePaymentId,
			amount,
			method,
			PaymentStatus.PAID,
			paidAt,
			null,
			null
		);
	}
	/**
	 * 결제 실패 결제 정보를 생성한다.
	 *
	 * 결제 실패 처리 API에서 사용할 수 있다.
	 * 현재 브랜치는 결제 승인 작업이지만,
	 * Payment 엔티티 자체는 실패 상태도 표현할 수 있어야 하므로 함께 둔다.
	 */
	public static Payment createFailedPayment(
		Long orderId,
		String portOnePaymentId,
		Long amount,
		String method,
		LocalDateTime failedAt,
		String failureReason
	) {
		return new Payment(
			orderId,
			PaymentOrderType.PROMOTION,
			portOnePaymentId,
			amount,
			method,
			PaymentStatus.FAILED,
			null,
			failedAt,
			failureReason
		);
	}


	/**
	 * 주문 유형을 명시해 결제 실패 정보를 생성한다.
	 */
	public static Payment createFailedPayment(
		Long orderId,
		PaymentOrderType orderType,
		String portOnePaymentId,
		Long amount,
		String method,
		LocalDateTime failedAt,
		String failureReason
	) {
		return new Payment(
			orderId,
			orderType,
			portOnePaymentId,
			amount,
			method,
			PaymentStatus.FAILED,
			null,
			failedAt,
			failureReason
		);
	}
	public boolean isPaid() {
		return this.status == PaymentStatus.PAID;
	}

	public boolean isFailed() {
		return this.status == PaymentStatus.FAILED;
	}

}
