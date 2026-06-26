package com.example.team8salecommerce.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;

import jakarta.persistence.LockModeType;

/**
 * 특가 상품 Repository
 *
 * PromotionProduct 엔티티를 DB에서 조회, 저장, 수정, 삭제할 때 사용한다.
 * JpaRepository를 상속하면 기본 CRUD 메서드를 바로 사용할 수 있다.
 */
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {

	/**
	 * 특가 상품을 조회하면서 쓰기 락을 획득한다.
	 *
	 * 결제 실패나 환불처럼 이벤트 재고를 복구하는 작업은
	 * 같은 특가 상품 재고를 동시에 수정할 수 있으므로 row lock으로 보호한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select promotionProduct
		from PromotionProduct promotionProduct
		where promotionProduct.id = :id
		""")
	Optional<PromotionProduct> findByIdForUpdate(@Param("id") Long id);
}
