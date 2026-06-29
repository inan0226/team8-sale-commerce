package com.example.team8salecommerce.domain.order.repository;

import com.example.team8salecommerce.domain.order.entity.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


 // 일반 주문 상품 영속성 처리를 담당

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 여러 주문의 일반 주문 상품을 원본 상품과 함께 일괄 조회
    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.product
            where oi.order.id in :orderIds
            order by oi.id
            """)
    List<OrderItem> findAllByOrderIdIn(Collection<Long> orderIds);

    // 하나의 주문에 포함된 일반 주문 상품을 조회
    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.product
            where oi.order.id = :orderId
            order by oi.id
            """)
    List<OrderItem> findAllByOrderId(Long orderId);
}
