package com.example.team8salecommerce.domain.order.entity;

import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.util.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 일반 주문 시점의 상품명, 가격, 수량을 보존하는 엔티티

@Getter
@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

	// 주문 상품 id
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	// 주문 시점 상품 스냅샷
	@Column(nullable = false)
	private String productName;

	// 주문 시점 상품 가격 스냅샷
	@Column(nullable = false)
	private Long productPrice;

	// 주문 수량
	@Column(nullable = false)
	private Integer quantity;

	private OrderItem(
		Order order,
		Product product,
		String productName,
		Long productPrice,
		Integer quantity
	) {
		validateCreate(order, product, productName, productPrice, quantity);
		this.order = order;
		this.product = product;
		this.productName = productName;
		this.productPrice = productPrice;
		this.quantity = quantity;
	}

	// 원본 상품 정보를 주문 스냅샷으로 생성
	public static OrderItem create(Order order, Product product, Integer quantity) {
		return new OrderItem(
			order,
			product,
			product.getName(),
			product.getPrice(),
			quantity
		);
	}

	public Long calculateTotalPrice() {
		return productPrice * quantity;
	}

	// 주문 생성 검증
	private void validateCreate(
		Order order,
		Product product,
		String productName,
		Long productPrice,
		Integer quantity
	) {
		if (order == null || product == null || productName == null || productPrice == null || quantity == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (productName.isBlank() || productPrice <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (quantity <= 0) {
			throw new CustomException(ErrorCode.INVALID_QUANTITY);
		}
	}
}