package com.example.team8salecommerce.domain.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.domain.stock.entity.StockHistory;
import com.example.team8salecommerce.domain.stock.repository.StockHistoryRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 결제 실패 처리 Service
 *
 * 결제 실패 정보를 저장하고,
 * 선착순 구매로 차감된 이벤트 재고를 복구하며,
 * 특가 주문 상태를 PAYMENT_FAILED로 변경한다.
 *
 * 결제 실패 처리 중 재고 복구가 중복으로 일어나면 안 되므로,
 * 주문 row lock을 획득한 뒤 주문 상태와 결제 내역을 검증한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentFailService {

	private final PaymentRepository paymentRepository;
	private final PromotionOrderRepository promotionOrderRepository;
	private final PromotionOrderItemRepository promotionOrderItemRepository;
	private final PromotionProductRepository promotionProductRepository;
	private final StockHistoryRepository stockHistoryRepository;

	/**
	 * 결제 실패 처리
	 *
	 * Payment 저장, 주문 상태 변경, 이벤트 재고 복구, 재고 이력 저장은
	 * 하나의 작업으로 묶여야 하므로 트랜잭션으로 처리한다.
	 */
	@Transactional
	public PaymentFailResponse failPayment(Long memberId, PaymentFailRequest request) {
		validateMemberId(memberId);

		PromotionOrder promotionOrder = findPromotionOrderForUpdate(
			memberId,
			request.orderId()
		);

		validatePromotionOrderCanBeFailed(promotionOrder);
		validateAlreadyPaidPayment(request.orderId());
		validateAlreadyFailedPayment(request.orderId());
		validateDuplicatedPortOnePayment(request.portOnePaymentId());
		validatePaymentAmount(promotionOrder, request.amount());

		PromotionOrderItem promotionOrderItem = findPromotionOrderItem(
			promotionOrder.getId()
		);

		PromotionProduct promotionProduct = findPromotionProduct(
			promotionOrder.getPromotionProductId()
		);

		LocalDateTime failedAt = LocalDateTime.now();

		Payment payment = Payment.createFailedPayment(
			promotionOrder.getId(),
			request.portOnePaymentId(),
			request.amount(),
			null,
			failedAt,
			request.failureReason()
		);

		Payment savedPayment = paymentRepository.save(payment);

		promotionOrder.failPayment(failedAt);

		restorePromotionStock(
			promotionProduct,
			promotionOrderItem,
			promotionOrder,
			savedPayment,
			failedAt
		);

		return PaymentFailResponse.of(
			savedPayment,
			promotionOrder.getStatus().name()
		);
	}

	/**
	 * 인증된 회원 ID를 검증한다.
	 */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}

	/**
	 * 본인 특가 주문을 조회하면서 row lock을 획득한다.
	 *
	 * 같은 주문에 대해 결제 실패 요청이 동시에 들어오면
	 * 재고가 중복 복구될 수 있으므로, 주문을 잠근 뒤 상태를 검증한다.
	 */
	private PromotionOrder findPromotionOrderForUpdate(Long memberId, Long orderId) {
		return promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 결제 실패 처리 가능한 주문 상태인지 검증한다.
	 *
	 * 선착순 구매 직후 결제 대기 상태인 주문만 실패 처리할 수 있다.
	 */
	private void validatePromotionOrderCanBeFailed(PromotionOrder promotionOrder) {
		if (!promotionOrder.isWaiting()) {
			throw new CustomException(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);
		}
	}

	/**
	 * 이미 결제 완료된 주문인지 검증한다.
	 *
	 * 결제 완료된 주문은 실패 처리로 재고를 복구하면 안 된다.
	 */
	private void validateAlreadyPaidPayment(Long orderId) {
		boolean alreadyPaid = paymentRepository.existsByOrderIdAndStatus(
			orderId,
			PaymentStatus.PAID
		);

		if (alreadyPaid) {
			throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
		}
	}

	/**
	 * 이미 결제 실패 처리된 주문인지 검증한다.
	 *
	 * 실패 결제가 중복 처리되면 이벤트 재고가 중복 복구될 수 있다.
	 */
	private void validateAlreadyFailedPayment(Long orderId) {
		boolean alreadyFailed = paymentRepository.existsByOrderIdAndStatus(
			orderId,
			PaymentStatus.FAILED
		);

		if (alreadyFailed) {
			throw new CustomException(ErrorCode.PAYMENT_ALREADY_FAILED);
		}
	}

	/**
	 * 같은 PortOne 결제 ID가 이미 저장되어 있는지 검증한다.
	 */
	private void validateDuplicatedPortOnePayment(String portOnePaymentId) {
		boolean duplicated = paymentRepository.findByPortOnePaymentId(portOnePaymentId)
			.isPresent();

		if (duplicated) {
			throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
		}
	}

	/**
	 * 주문 금액과 결제 실패 요청 금액이 일치하는지 검증한다.
	 */
	private void validatePaymentAmount(
		PromotionOrder promotionOrder,
		Long paymentAmount
	) {
		if (!promotionOrder.getTotalAmount().equals(paymentAmount)) {
			throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}
	}

	/**
	 * 특가 주문 상품을 조회한다.
	 *
	 * 결제 실패 시 몇 개의 이벤트 재고를 복구해야 하는지 알기 위해 필요하다.
	 */
	private PromotionOrderItem findPromotionOrderItem(Long promotionOrderId) {
		return promotionOrderItemRepository.findByPromotionOrderId(promotionOrderId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 특가 상품을 조회한다.
	 *
	 * 결제 실패 시 이벤트 재고를 복구할 대상이다.
	 */
	private PromotionProduct findPromotionProduct(Long promotionProductId) {
		return promotionProductRepository.findById(promotionProductId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_PRODUCT_NOT_FOUND));
	}

	/**
	 * 결제 실패로 선점된 이벤트 재고를 복구하고 재고 이력을 저장한다.
	 */
	private void restorePromotionStock(
		PromotionProduct promotionProduct,
		PromotionOrderItem promotionOrderItem,
		PromotionOrder promotionOrder,
		Payment payment,
		LocalDateTime now
	) {
		Integer stockBefore = promotionProduct.getRemainingEventStock();

		promotionProduct.restoreStock(promotionOrderItem.getQuantity(), now);

		Integer stockAfter = promotionProduct.getRemainingEventStock();

		StockHistory stockHistory = StockHistory.createPaymentFailRestoreHistory(
			promotionOrderItem.getProductId(),
			promotionProduct.getId(),
			promotionOrder.getId(),
			payment.getId(),
			promotionOrderItem.getQuantity(),
			stockBefore,
			stockAfter
		);

		stockHistoryRepository.save(stockHistory);
	}
}
