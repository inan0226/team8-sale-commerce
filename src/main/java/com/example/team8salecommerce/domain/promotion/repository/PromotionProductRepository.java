package com.example.team8salecommerce.domain.promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;

/**
 * 특가 상품 Repository
 *
 * PromotionProduct 엔티티를 DB에서 조회, 저장, 수정, 삭제할 때 사용한다.
 * JpaRepository를 상속하면 기본 CRUD 메서드를 바로 사용할 수 있다.
 */
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {
}
