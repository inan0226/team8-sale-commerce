package com.example.team8salecommerce.domain.refund.facade;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.team8salecommerce.domain.refund.client.PortOneRefundClient;
import com.example.team8salecommerce.domain.refund.client.PortOneRefundResult;
import com.example.team8salecommerce.domain.refund.dto.RefundRequest;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.service.RefundCompleteTransactionService;
import com.example.team8salecommerce.domain.refund.service.RefundFailTransactionService;
import com.example.team8salecommerce.domain.refund.service.RefundProcessingContext;
import com.example.team8salecommerce.domain.refund.service.RefundRequestTransactionService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 환불 Facade
 *
 * 환불 요청부터 PortOne 환불 요청,
 * 내부 환불 완료 처리와 재고 복구까지 전체 흐름을 조합한다.
 *
 * 외부 API 호출은 DB 트랜잭션 밖에서 수행하고,
 * DB 상태 변경은 각 TransactionService에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class RefundFacade {

	private static final String PORTONE_REFUND_FAILED_MESSAGE = "PortOne 환불 요청에 실패했습니다.";
	private static final String PORTONE_REFUND_NOT_SUCCEEDED_MESSAGE = "PortOne 환불이 성공 상태가 아닙니다.";

	private final RefundRequestTransactionService refundRequestTransactionService;
	private final PortOneRefundClient portOneRefundClient;
	private final RefundCompleteTransactionService refundCompleteTransactionService;
	private final RefundFailTransactionService refundFailTransactionService;

	/**
	 * 환불 요청 처리
	 *
	 * 1. 환불 요청 생성 + 주문 REFUND_REQUEST 변경
	 * 2. PortOne 결제 취소 API 호출
	 * 3. 성공 시 내부 환불 완료 + 주문 REFUNDED + 재고 복구
	 * 4. 실패 시 환불 실패 + 주문 PAID 복구
	 */
	public RefundResponse requestRefund(
		Long memberId,
		Long orderId,
		RefundRequest request
	) {
		RefundProcessingContext context = refundRequestTransactionService.requestRefund(
			memberId,
			orderId,
			request
		);

		PortOneRefundResult refundResult = requestPortOneRefund(context);

		if (!refundResult.isSucceeded()) {
			refundFailTransactionService.failRefund(
				context,
				PORTONE_REFUND_NOT_SUCCEEDED_MESSAGE
			);

			throw new CustomException(ErrorCode.REFUND_FAILED);
		}

		return refundCompleteTransactionService.completeRefund(context);
	}

	/**
	 * PortOne 환불 요청
	 *
	 * 이 메서드는 트랜잭션 밖에서 호출된다.
	 * PortOne 호출 실패 시 내부 상태를 환불 실패로 변경하고 예외를 다시 던진다.
	 */
	private PortOneRefundResult requestPortOneRefund(RefundProcessingContext context) {
		try {
			return portOneRefundClient.refund(
				context.portOnePaymentId(),
				context.refundAmount(),
				createRefundReason(context)
			);
		} catch (CustomException e) {
			refundFailTransactionService.failRefund(
				context,
				PORTONE_REFUND_FAILED_MESSAGE
			);

			throw e;
		} catch (RuntimeException e) {
			refundFailTransactionService.failRefund(
				context,
				PORTONE_REFUND_FAILED_MESSAGE
			);

			throw new CustomException(ErrorCode.REFUND_FAILED);
		}
	}

	/**
	 * PortOne에 전달할 환불 사유를 생성한다.
	 */
	private String createRefundReason(RefundProcessingContext context) {
		if (!StringUtils.hasText(context.reasonDetail())) {
			return context.reasonType().name();
		}

		return context.reasonType().name() + " - " + context.reasonDetail();
	}
}
