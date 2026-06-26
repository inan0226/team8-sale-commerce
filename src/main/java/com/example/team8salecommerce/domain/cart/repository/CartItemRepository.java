package com.example.team8salecommerce.domain.cart.repository;

import com.example.team8salecommerce.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("""
            select ci
            from CartItem ci
            join fetch ci.product
            where ci.cart.id = :cartId
              and ci.deletedAt is null
            """)
    // 장바구니 상품 목록 조회
    List<CartItem> findActiveCartItems(Long cartId);


    // 장바구니와 상품으로 장바구니 상품 조회
    Optional<CartItem> findByCartIdAndProductIdAndDeletedAtIsNull(
            Long cartId,
            Long productId
    );

    Optional<CartItem> findByIdAndDeletedAtIsNull(
            Long cartItemId
    );

    Optional<CartItem> findByCartIdAndProductId(
            Long cartId,
            Long productId
    );

}
