package com.example.team8salecommerce.domain.order.repository;

import com.example.team8salecommerce.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;


 // 공통 주문 영속성 처리를 담당

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원의 주문 목록을 최근 주문부터 조회
    List<Order> findAllByMemberIdOrderByOrderedAtDesc(Long memberId);

    // 취소 중 중복 상태 변경을 막기 위해 회원 소유 주문을 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            where o.id = :orderId
              and o.member.id = :memberId
            """)
    Optional<Order> findByIdAndMemberIdForUpdate(Long orderId, Long memberId);
}
