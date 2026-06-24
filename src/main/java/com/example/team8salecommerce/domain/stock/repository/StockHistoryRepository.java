package com.example.team8salecommerce.domain.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team8salecommerce.domain.stock.entity.StockHistory;

/**
 * 재고 변경 이력 Repository
 *
 * StockHistory 엔티티를 DB에 저장하거나 조회할 때 사용한다.
 */
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
}
