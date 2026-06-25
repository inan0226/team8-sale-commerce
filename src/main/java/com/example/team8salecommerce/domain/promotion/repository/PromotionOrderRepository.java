package com.example.team8salecommerce.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;

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
}
