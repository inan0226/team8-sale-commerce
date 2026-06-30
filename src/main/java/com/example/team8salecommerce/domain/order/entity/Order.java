package com.example.team8salecommerce.domain.order.entity;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.util.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 일반 상품과 특가 상품 주문이 공유하는 주문 대표 엔티티

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	// 주문 id
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 주문한 회원
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	// 주문 상품 가격 합계
	@Column(nullable = false)
	private Long totalPrice;

	// 주문 처리 상태
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	// 주문 요청 시간
	@Column(nullable = false)
	private LocalDateTime orderedAt;

	private Order(Member member, Long totalPrice, LocalDateTime orderedAt) {
		validateCreate(member, totalPrice, orderedAt);
		this.member = member;
		this.totalPrice = totalPrice;
		this.status = OrderStatus.WAITING;
		this.orderedAt = orderedAt;
	}

	// 결제 전 새 주문 생성
	public static Order create(Member member, Long totalPrice, LocalDateTime orderedAt) {
		return new Order(member, totalPrice, orderedAt);
	}

	// 주문 취소
	public void cancel() {
		if (status != OrderStatus.WAITING) {
			throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
		}

		this.status = OrderStatus.CANCELLED;
	}

	// 결제 대기 주문을 결제 완료 상태로 변경
	public void markAsPaid() {
		if (!isWaiting()) {
			throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
		}
		this.status = OrderStatus.PAID;
	}

	// 결제 대기 주문을 결제 실패 상태로 변경
	public void failPayment() {
		if (!isWaiting()) {
			throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
		}
		this.status = OrderStatus.PAYMENT_FAILED;
	}

	// 결제 대기 상태 여부 확인
	public boolean isWaiting() {
		return status == OrderStatus.WAITING;
	}

	// 결제 완료 상태 여부 확인
	public boolean isPaid() {
		return status == OrderStatus.PAID;
	}

	// 주문 생성 검증
	private void validateCreate(Member member, Long totalPrice, LocalDateTime orderedAt) {
		if (member == null || totalPrice == null || orderedAt == null || totalPrice <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
