package com.example.team8salecommerce.domain.cart.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.team8salecommerce.domain.cart.entity.CartItem;

import jakarta.persistence.LockModeType;

/** 장바구니 상품 영속성 처리를 담당한다. */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	/** 활성 장바구니 상품과 상품 정보를 한 번에 조회한다. */
	@Query("""
		select ci
		from CartItem ci
		join fetch ci.product
		where ci.cart.id = :cartId
		  and ci.deletedAt is null
		""")
	List<CartItem> findActiveCartItems(Long cartId);

	/** 활성 장바구니 상품을 장바구니와 상품 ID로 조회한다. */
	Optional<CartItem> findByCartIdAndProductIdAndDeletedAtIsNull(Long cartId, Long productId);

	/** 활성 장바구니 상품을 식별자로 조회한다. */
	Optional<CartItem> findByIdAndDeletedAtIsNull(Long cartItemId);

	/** 활성 상품에 연결된 장바구니 항목을 잠금 조회한다. 삭제된 항목도 복구 대상으로 포함한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select ci
		from CartItem ci
		join fetch ci.product p
		where ci.cart.id = :cartId
		  and p.id = :productId
		  and p.isDeleted = false
		""")
	Optional<CartItem> findByCartIdAndProductIdWithActiveProduct(Long cartId, Long productId);

	/** 주문 요청에 포함된 활성 장바구니 상품과 상품 정보를 잠금 조회한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select ci
		from CartItem ci
		join fetch ci.product
		where ci.cart.id = :cartId
		  and ci.id in :cartItemIds
		  and ci.deletedAt is null
		""")
	List<CartItem> findActiveCartItemsByIds(Long cartId, Collection<Long> cartItemIds);

	/** 주문된 활성 장바구니 항목을 한 번의 조건부 UPDATE로 소프트 삭제한다. */
	@Modifying
	@Query("""
		update CartItem ci
		set ci.deletedAt = :deletedAt
		where ci.cart.id = :cartId
		  and ci.id in :cartItemIds
		  and ci.deletedAt is null
		""")
	int softDeleteActiveByIds(
		@Param("cartId") Long cartId,
		@Param("cartItemIds") Collection<Long> cartItemIds,
		@Param("deletedAt") LocalDateTime deletedAt
	);
}
