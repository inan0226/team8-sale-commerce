package com.example.team8salecommerce.domain.order.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.team8salecommerce.domain.order.entity.OrderItem;

/** 일반 주문 상품 영속성 처리를 담당한다. */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	/** 여러 주문의 주문 상품을 한 번에 조회한다. */
	@Query("""
		select oi
		from OrderItem oi
		where oi.order.id in :orderIds
		order by oi.id
		""")
	List<OrderItem> findAllByOrderIdIn(Collection<Long> orderIds);

	/** 하나의 주문에 포함된 주문 상품을 조회한다. */
	@Query("""
		select oi
		from OrderItem oi
		where oi.order.id = :orderId
		order by oi.id
		""")
	List<OrderItem> findAllByOrderId(Long orderId);
}
