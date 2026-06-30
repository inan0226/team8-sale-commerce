package com.example.team8salecommerce.domain.order.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.team8salecommerce.domain.product.entity.Product;

import jakarta.persistence.LockModeType;

/** 주문 처리에 필요한 상품 재고 변경을 담당한다. */
public interface OrderProductRepository extends JpaRepository<Product, Long> {

	/** 결제 실패 복구가 사용하는 원본 상품 잠금 조회를 유지한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select p
		from Product p
		where p.id in :productIds
		order by p.id
		""")
	List<Product> findAllByIdInForUpdate(@Param("productIds") Collection<Long> productIds);

	/** 현재 재고가 주문 수량 이상인 활성 상품의 재고를 원자적으로 차감한다. */
	@Modifying
	@Query("""
		update Product p
		set p.stock = p.stock - :quantity
		where p.id = :productId
		  and p.isDeleted = false
		  and p.stock >= :quantity
		""")
	int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

	/** 주문 취소 수량만큼 원본 상품 재고를 복구한다. */
	@Modifying
	@Query("""
		update Product p
		set p.stock = p.stock + :quantity
		where p.id = :productId
		""")
	int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
