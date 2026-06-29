package com.example.team8salecommerce.domain.order.repository;

import com.example.team8salecommerce.domain.order.entity.PromotionOrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


 // 공통 주문에 속한 특가 주문 상품 영속성 처리를 담당

public interface CommonPromotionOrderItemRepository extends JpaRepository<PromotionOrderItem, Long> {

    // 여러 공통 주문의 특가 주문 상품을 일괄 조회
    @Query("""
            select poi
            from CommonPromotionOrderItem poi
            join fetch poi.product
            join fetch poi.promotionProduct
            where poi.order.id in :orderIds
            order by poi.id
            """)
    List<PromotionOrderItem> findAllByOrderIdIn(Collection<Long> orderIds);
}
