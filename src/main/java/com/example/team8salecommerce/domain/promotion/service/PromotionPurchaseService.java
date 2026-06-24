package com.example.team8salecommerce.domain.promotion.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseRequest;
import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseResponse;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 선착순 특가 구매 Service
 *
 * Redis Lock을 획득한 뒤 실제 구매 관련 DB 변경을 처리한다.
 *
 * Service의 역할:
 * - 특가 상품 조회
 * - 구매 가능 여부 검증
 * - 이벤트 재고 차감
 * - 특가 주문 생성
 * - 특가 주문 상품 생성
 * - 응답 DTO 반환
 */
@Service
@RequiredArgsConstructor
public class PromotionPurchaseService {

	private final PromotionProductRepository promotionProductRepository;
	private final PromotionOrderRepository promotionOrderRepository;
	private final PromotionOrderItemRepository promotionOrderItemRepository;

	/**
	 * 선착순 구매를 트랜잭션 안에서 처리한다.
	 *
	 * 이 메서드는 Facade에서 Redis Lock을 획득한 뒤 호출된다.
	 * 따라서 이벤트 재고 조회와 차감이 동시에 실행되지 않도록 보호된다.
	 */
	@Transactional
	public PromotionPurchaseResponse purchaseWithTransaction(
		Long memberId,
		Long promotionProductId,
		PromotionPurchaseRequest request
	) {
		validateRequest(memberId, promotionProductId, request);

		LocalDateTime now = LocalDateTime.now();

		PromotionProduct promotionProduct = findPromotionProduct(promotionProductId);

		promotionProduct.validatePurchasable(now, request.quantity());
		promotionProduct.decreaseStock(request.quantity());

		Long totalAmount = calculateTotalAmount(
			promotionProduct.getPromotionPrice(),
			request.quantity()
		);

		PromotionOrder promotionOrder = createPromotionOrder(
			memberId,
			promotionProduct.getId(),
			totalAmount,
			now
		);

		createPromotionOrderItem(
			promotionOrder.getId(),
			promotionProduct,
			request.quantity()
		);

		return PromotionPurchaseResponse.of(
			promotionOrder.getId(),
			promotionProduct,
			request.quantity(),
			promotionOrder.getStatus().name()
		);
	}

	/**
	 * 구매 요청에 필요한 기본값을 검증한다.
	 */
	private void validateRequest(
		Long memberId,
		Long promotionProductId,
		PromotionPurchaseRequest request
	) {
		if (memberId == null || promotionProductId == null || request == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (request.quantity() == null || request.quantity() <= 0) {
			throw new CustomException(ErrorCode.INVALID_QUANTITY);
		}
	}

	/**
	 * 특가 상품을 조회한다.
	 */
	private PromotionProduct findPromotionProduct(Long promotionProductId) {
		return promotionProductRepository.findById(promotionProductId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_PRODUCT_NOT_FOUND));
	}

	/**
	 * 특가 주문을 생성하고 저장한다.
	 */
	private PromotionOrder createPromotionOrder(
		Long memberId,
		Long promotionProductId,
		Long totalAmount,
		LocalDateTime orderedAt
	) {
		PromotionOrder promotionOrder = PromotionOrder.create(
			memberId,
			promotionProductId,
			totalAmount,
			orderedAt
		);

		return promotionOrderRepository.save(promotionOrder);
	}

	/**
	 * 특가 주문 상품을 생성하고 저장한다.
	 *
	 * 상품명과 가격은 주문 시점 기준으로 저장한다.
	 */
	private void createPromotionOrderItem(
		Long promotionOrderId,
		PromotionProduct promotionProduct,
		Integer quantity
	) {
		PromotionOrderItem promotionOrderItem = PromotionOrderItem.create(
			promotionOrderId,
			promotionProduct.getId(),
			promotionProduct.getProductId(),
			promotionProduct.getTitle(),
			quantity,
			promotionProduct.getPromotionPrice()
		);

		promotionOrderItemRepository.save(promotionOrderItem);
	}

	/**
	 * 총 주문 금액을 계산한다.
	 *
	 * 특가 단가 * 구매 수량
	 */
	private Long calculateTotalAmount(Long unitPrice, Integer quantity) {
		if (unitPrice == null || unitPrice <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		if (quantity == null || quantity <= 0) {
			throw new CustomException(ErrorCode.INVALID_QUANTITY);
		}

		return unitPrice * quantity;
	}
}
