package com.example.team8salecommerce.domain.refund.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.entity.RefundStatus;

import jakarta.persistence.LockModeType;

public interface RefundRepository extends JpaRepository<Refund, Long> {

	/**
	 * 주문 ID로 환불 정보를 조회한다.
	 *
	 * 한 주문에 이미 환불 요청 또는 환불 완료 내역이 있는지 확인할 때 사용한다.
	 */
	Optional<Refund> findByOrderId(Long orderId);

	/**
	 * 특정 주문에 특정 상태의 환불 정보가 존재하는지 확인한다.
	 *
	 * 중복 환불 방지 로직에서 사용할 수 있다.
	 * 예: 이미 REFUNDED 상태인 환불이 있으면 다시 환불 불가
	 */
	boolean existsByOrderIdAndStatus(Long orderId, RefundStatus status);

	/**
	 * 회원 ID와 환불 ID로 환불 정보를 조회한다.
	 *
	 * 환불 상세 조회에서 본인의 환불 내역인지 확인할 때 사용할 수 있다.
	 */
	Optional<Refund> findByIdAndMemberId(Long refundId, Long memberId);

	/**
	 * 환불 ID로 환불 정보를 조회하면서 쓰기 락을 획득한다.
	 *
	 * PortOne 환불 성공/실패 후 내부 환불 상태를 변경할 때
	 * 같은 환불 row가 동시에 수정되지 않도록 보호한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select refund
		from Refund refund
		where refund.id = :id
		""")
	Optional<Refund> findByIdForUpdate(@Param("id") Long id);
}
