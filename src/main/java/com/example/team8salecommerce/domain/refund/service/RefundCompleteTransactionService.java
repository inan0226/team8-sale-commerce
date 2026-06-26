package com.example.team8salecommerce.domain.refund.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.domain.stock.entity.StockHistory;
import com.example.team8salecommerce.domain.stock.repository.StockHistoryRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 환불 완료 트랜잭션 Service
 *
 * PortOne 환불 요청이 성공한 뒤 내부 상태를 최종 완료 처리한다.
 *
 * 환불 완료, 주문 환불 완료, 이벤트 재고 복구, 재고 이력 저장을
 * 하나의 트랜잭션으로 묶어서 정합성을 맞춘다.
 */
@Service
@RequiredArgsConstructor
public class RefundCompleteTransactionService {

	private final RefundRepository refundRepository;
	private final PromotionOrderRepository promotionOrderRepository;
	private final PromotionOrderItemRepository promotionOrderItemRepository;
	private final PromotionProductRepository promotionProductRepository;
	private final StockHistoryRepository stockHistoryRepository;

	/**
	 * 환불 성공 후 내부 상태를 완료 처리한다.
	 */
	@Transactional
	public RefundResponse completeRefund(RefundProcessingContext context) {
		validateContext(context);

		Refund refund = findRefundForUpdate(context.refundId());
		validateRefundRequested(refund);

		PromotionOrder promotionOrder = findPromotionOrderForUpdate(refund);
		PromotionOrderItem orderItem = findPromotionOrderItem(refund.getOrderId());
		PromotionProduct promotionProduct = findPromotionProductForUpdate(orderItem.getPromotionProductId());

		LocalDateTime completedAt = LocalDateTime.now();

		Integer restoreQuantity = orderItem.getQuantity();
		Integer stockBefore = promotionProduct.getRemainingEventStock();

		promotionProduct.restoreStock(restoreQuantity, completedAt);

		Integer stockAfter = promotionProduct.getRemainingEventStock();

		StockHistory stockHistory = StockHistory.createRefundRestoreHistory(
			orderItem.getProductId(),
			orderItem.getPromotionProductId(),
			refund.getOrderId(),
			refund.getPaymentId(),
			refund.getId(),
			restoreQuantity,
			stockBefore,
			stockAfter
		);

		stockHistoryRepository.save(stockHistory);

		refund.complete(completedAt);
		promotionOrder.completeRefund(completedAt);

		return RefundResponse.of(refund, restoreQuantity, stockAfter);
	}

	/**
	 * 환불 처리 context를 검증한다.
	 */
	private void validateContext(RefundProcessingContext context) {
		if (context == null || context.refundId() == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}

	/**
	 * 환불 row를 조회하면서 쓰기 락을 획득한다.
	 */
	private Refund findRefundForUpdate(Long refundId) {
		return refundRepository.findByIdForUpdate(refundId)
			.orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));
	}

	/**
	 * 환불 요청 상태인지 검증한다.
	 */
	private void validateRefundRequested(Refund refund) {
		if (!refund.isRequested()) {
			throw new CustomException(ErrorCode.REFUND_NOT_ALLOWED);
		}
	}

	/**
	 * 환불 대상 주문을 조회하면서 쓰기 락을 획득한다.
	 */
	private PromotionOrder findPromotionOrderForUpdate(Refund refund) {
		return promotionOrderRepository.findByIdAndMemberIdForUpdate(refund.getOrderId(), refund.getMemberId())
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 환불 대상 주문 상품을 조회한다.
	 */
	private PromotionOrderItem findPromotionOrderItem(Long orderId) {
		return promotionOrderItemRepository.findByPromotionOrderId(orderId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_ITEM_NOT_FOUND));
	}

	/**
	 * 환불 대상 특가 상품을 조회하면서 쓰기 락을 획득한다.
	 *
	 * 같은 특가 상품 재고가 동시에 복구되는 경우 lost update를 막기 위해 필요하다.
	 */
	private PromotionProduct findPromotionProductForUpdate(Long promotionProductId) {
		return promotionProductRepository.findByIdForUpdate(promotionProductId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_PRODUCT_NOT_FOUND));
	}
}
