package com.example.team8salecommerce.domain.cart.repository;

import com.example.team8salecommerce.domain.cart.entity.CartItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * 장바구니 상품 영속성 처리를 담당한다.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * 장바구니의 활성 상품 목록을 상품 정보와 함께 조회한다.
     */
    @Query("""
            select ci
            from CartItem ci
            join fetch ci.product
            where ci.cart.id = :cartId
              and ci.deletedAt is null
            """)
    List<CartItem> findActiveCartItems(Long cartId);

    /**
     * 활성 장바구니 상품을 장바구니와 상품 ID로 조회한다.
     */
    Optional<CartItem> findByCartIdAndProductIdAndDeletedAtIsNull(
            Long cartId,
            Long productId
    );

    /**
     * 활성 장바구니 상품을 식별자로 조회한다.
     */
    Optional<CartItem> findByIdAndDeletedAtIsNull(Long cartItemId);

    /**
     * 삭제 여부와 관계없이 장바구니와 상품 ID로 조회한다.
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * 주문 요청에 포함된 활성 장바구니 상품과 상품 정보를 한 번에 조회한다.
     * 동일한 장바구니 항목으로 동시에 주문하는 요청은 이 잠금에서 직렬화된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ci
            from CartItem ci
            join fetch ci.product
            where ci.cart.id = :cartId
              and ci.id in :cartItemIds
              and ci.deletedAt is null
            """)
    List<CartItem> findActiveCartItemsByIds(
            Long cartId,
            Collection<Long> cartItemIds
    );
}
