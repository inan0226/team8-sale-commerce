package com.example.team8salecommerce.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;

/**
 * 결제 Repository
 *
 * Payment 엔티티를 DB에 저장하거나 조회할 때 사용한다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/**
	 * 주문 ID로 결제 정보를 조회한다.
	 *
	 * 주문별 결제 정보 조회, 중복 결제 여부 확인,
	 * 환불 시 결제 정보 조회 등에 사용할 수 있다.
	 */
	Optional<Payment> findByOrderId(Long orderId);

	/**
	 * PortOne 결제 ID로 결제 정보를 조회한다.
	 *
	 * 같은 PortOne 결제 ID가 중복 저장되는 것을 방지하거나,
	 * 외부 결제 ID 기준으로 결제 정보를 찾을 때 사용할 수 있다.
	 */
	Optional<Payment> findByPortOnePaymentId(String portOnePaymentId);

	/**
	 * 특정 주문에 이미 결제 완료 내역이 있는지 확인한다.
	 *
	 * 결제 승인 API에서 중복 결제를 막을 때 사용할 수 있다.
	 */
	boolean existsByOrderIdAndStatus(Long orderId, com.example.team8salecommerce.domain.payment.entity.PaymentStatus status);

	/**
	 * 주문 ID와 결제 상태로 결제 정보를 조회한다.
	 *
	 * 환불 요청 시 결제 완료된 Payment만 환불 대상으로 사용하기 위해 필요하다.
	 */
	Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
