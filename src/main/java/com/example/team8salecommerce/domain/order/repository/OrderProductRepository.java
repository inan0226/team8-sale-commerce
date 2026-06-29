package com.example.team8salecommerce.domain.order.repository;

import com.example.team8salecommerce.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


 // 주문 처리에 필요한 상품 잠금과 재고 변경만 담당하는 주문 도메인 저장소

public interface OrderProductRepository extends JpaRepository<Product, Long> {


    // 주문 생성 대상인 활성 상품을 ID 순서대로 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Product p
            where p.id in :productIds
              and p.isDeleted = false
            order by p.id
            """)
    List<Product> findAllActiveByIdInForUpdate(
            @Param("productIds") Collection<Long> productIds
    );


    // 주문 취소 시 삭제 여부와 관계없이 원본 상품을 잠금 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Product p
            where p.id in :productIds
            order by p.id
            """)
    List<Product> findAllByIdInForUpdate(
            @Param("productIds") Collection<Long> productIds
    );


    // 현재 재고가 주문 수량 이상일 때만 원자적으로 재고를 차감
    @Modifying(flushAutomatically = true)
    @Query("""
            update Product p
            set p.stock = p.stock - :quantity
            where p.id = :productId
              and p.isDeleted = false
              and p.stock >= :quantity
            """)
    int decreaseStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );


    // 주문 취소 수량만큼 원본 상품 재고를 복구

    @Modifying(flushAutomatically = true)
    @Query("""
            update Product p
            set p.stock = p.stock + :quantity
            where p.id = :productId
            """)
    int restoreStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );
}