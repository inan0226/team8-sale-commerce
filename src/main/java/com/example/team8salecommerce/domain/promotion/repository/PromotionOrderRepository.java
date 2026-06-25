package com.example.team8salecommerce.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;

import jakarta.persistence.LockModeType;

/**
 * 특가 주문 Repository
 *
 * 특가 주문 조회, 저장을 담당한다.
 */
public interface PromotionOrderRepository extends JpaRepository<PromotionOrder, Long> {

	/**
	 * 특가 주문 ID와 회원 ID로 주문을 조회한다.
	 *
	 * 환불 요청이나 주문 상세 조회에서
	 * 본인 주문인지 확인할 때 사용한다.
	 */
	Optional<PromotionOrder> findByIdAndMemberId(Long id, Long memberId);

	/**
	 * 특가 주문 ID와 회원 ID로 주문을 조회하면서 쓰기 락을 획득한다.
	 *
	 * 같은 주문에 대해 서로 다른 PortOne 결제 ID로
	 * 동시에 결제 승인 요청이 들어오는 경우를 방어하기 위해 사용한다.
	 *
	 * 첫 번째 요청이 주문 상태를 PAID로 변경하고 커밋하면,
	 * 이후 대기 중이던 요청은 변경된 주문 상태를 확인하고 중복 결제로 실패한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
 select promotionOrder
 from PromotionOrder promotionOrder
 where promotionOrder.id = :id
 and promotionOrder.memberId = :memberId
 """)
	Optional<PromotionOrder> findByIdAndMemberIdForUpdate(
		@Param("id") Long id,
		@Param("memberId") Long memberId
	);
}
