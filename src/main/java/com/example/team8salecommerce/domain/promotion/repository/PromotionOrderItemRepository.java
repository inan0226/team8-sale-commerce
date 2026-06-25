package com.example.team8salecommerce.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;

/**
 * 특가 주문 상품 Repository
 *
 * 특가 주문에 포함된 상품 정보를 조회, 저장한다.
 */
public interface PromotionOrderItemRepository extends JpaRepository<PromotionOrderItem, Long> {

	/**
	 * 특가 주문 ID로 주문 상품을 조회한다.
	 *
	 * 현재 선착순 특가 구매는 한 번에 하나의 특가 상품만 구매하는 구조라
	 * Optional로 단건 조회한다.
	 */
	Optional<PromotionOrderItem> findByPromotionOrderId(Long promotionOrderId);
}
