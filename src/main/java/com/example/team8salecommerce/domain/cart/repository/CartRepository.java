package com.example.team8salecommerce.domain.cart.repository;

import com.example.team8salecommerce.domain.cart.entity.Cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

	// 회원 id로 장바구니 조회
	Optional<Cart> findByMemberId(Long memberId);

}
