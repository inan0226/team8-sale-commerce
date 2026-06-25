package com.example.team8salecommerce.domain.promotion.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.team8salecommerce.global.exception.CustomException;

/**
 * 특가 주문 상품 Entity 테스트
 *
 * 선착순 구매 시 생성되는 주문 상품 스냅샷 정보를 테스트한다.
 */
class PromotionOrderItemTest {

	@Test
	@DisplayName("특가 주문 상품 생성에 성공한다")
	void createPromotionOrderItemSuccess() {
		// given
		Long promotionOrderId = 1L;
		Long promotionProductId = 10L;
		Long productId = 100L;
		String productName = "테스트 상품";
		Integer quantity = 2;
		Long unitPrice = 10000L;

		// when
		PromotionOrderItem promotionOrderItem = PromotionOrderItem.create(
			promotionOrderId,
			promotionProductId,
			productId,
			productName,
			quantity,
			unitPrice
		);

		// then
		assertThat(promotionOrderItem.getPromotionOrderId()).isEqualTo(promotionOrderId);
		assertThat(promotionOrderItem.getPromotionProductId()).isEqualTo(promotionProductId);
		assertThat(promotionOrderItem.getProductId()).isEqualTo(productId);
		assertThat(promotionOrderItem.getProductName()).isEqualTo(productName);
		assertThat(promotionOrderItem.getQuantity()).isEqualTo(quantity);
		assertThat(promotionOrderItem.getUnitPrice()).isEqualTo(unitPrice);
		assertThat(promotionOrderItem.getTotalPrice()).isEqualTo(20000L);
	}

	@Test
	@DisplayName("구매 수량이 0 이하이면 특가 주문 상품 생성에 실패한다")
	void createPromotionOrderItemFailWhenQuantityIsZeroOrNegative() {
		// when & then
		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			100L,
			"테스트 상품",
			0,
			10000L
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			100L,
			"테스트 상품",
			-1,
			10000L
		)).isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("상품 단가가 0 이하이면 특가 주문 상품 생성에 실패한다")
	void createPromotionOrderItemFailWhenUnitPriceIsZeroOrNegative() {
		// when & then
		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			100L,
			"테스트 상품",
			1,
			0L
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			100L,
			"테스트 상품",
			1,
			-1000L
		)).isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("상품명이 비어 있으면 특가 주문 상품 생성에 실패한다")
	void createPromotionOrderItemFailWhenProductNameIsBlank() {
		// when & then
		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			100L,
			"",
			1,
			10000L
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			100L,
			"   ",
			1,
			10000L
		)).isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("필수 ID 값이 null이면 특가 주문 상품 생성에 실패한다")
	void createPromotionOrderItemFailWhenRequiredIdIsNull() {
		// when & then
		assertThatThrownBy(() -> PromotionOrderItem.create(
			null,
			10L,
			100L,
			"테스트 상품",
			1,
			10000L
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			null,
			100L,
			"테스트 상품",
			1,
			10000L
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> PromotionOrderItem.create(
			1L,
			10L,
			null,
			"테스트 상품",
			1,
			10000L
		)).isInstanceOf(CustomException.class);
	}
}
