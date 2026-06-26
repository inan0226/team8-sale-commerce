package com.example.team8salecommerce.domain.refund.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.team8salecommerce.domain.refund.client.PortOneRefundClient;
import com.example.team8salecommerce.domain.refund.client.PortOneRefundResult;
import com.example.team8salecommerce.domain.refund.dto.RefundRequest;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;
import com.example.team8salecommerce.domain.refund.service.RefundCompleteTransactionService;
import com.example.team8salecommerce.domain.refund.service.RefundFailTransactionService;
import com.example.team8salecommerce.domain.refund.service.RefundPortOneSuccessTransactionService;
import com.example.team8salecommerce.domain.refund.service.RefundProcessingContext;
import com.example.team8salecommerce.domain.refund.service.RefundRequestTransactionService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * RefundFacade 테스트
 *
 * 환불 요청 전체 흐름을 검증한다.
 *
 * Facade는 아래 순서를 조합한다.
 * 1. 환불 요청 생성 트랜잭션
 * 2. PortOne 환불 요청
 * 3. PortOne 성공 시 PortOne 환불 성공 정보 저장 트랜잭션
 * 4. PortOne 성공 정보 저장 이후 내부 환불 완료 트랜잭션
 * 5. PortOne 실패 시 내부 환불 실패 트랜잭션
 */
@ExtendWith(MockitoExtension.class)
class RefundFacadeTest {

	@Mock
	private RefundRequestTransactionService refundRequestTransactionService;

	@Mock
	private PortOneRefundClient portOneRefundClient;

	@Mock
	private RefundPortOneSuccessTransactionService refundPortOneSuccessTransactionService;

	@Mock
	private RefundCompleteTransactionService refundCompleteTransactionService;

	@Mock
	private RefundFailTransactionService refundFailTransactionService;

	@InjectMocks
	private RefundFacade refundFacade;

	@Test
	@DisplayName("PortOne 환불 성공 시 PortOne 성공 정보 저장 후 내부 환불 완료 처리 결과를 반환한다")
	void requestRefundSuccess() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;
		Long refundAmount = 7000L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		RefundProcessingContext context = createContext(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		RefundResponse expectedResponse = new RefundResponse(
			1L,
			orderId,
			20L,
			refundAmount,
			"REFUNDED",
			1,
			10,
			LocalDateTime.now(),
			LocalDateTime.now()
		);

		PortOneRefundResult refundResult = new PortOneRefundResult(
			"cancel-123",
			"SUCCEEDED"
		);

		when(refundRequestTransactionService.requestRefund(memberId, orderId, request))
			.thenReturn(context);

		when(portOneRefundClient.refund(
			"payment-123",
			refundAmount,
			"CHANGE_OF_MIND - 단순 변심"
		)).thenReturn(refundResult);

		when(refundCompleteTransactionService.completeRefund(context))
			.thenReturn(expectedResponse);

		// when
		RefundResponse response = refundFacade.requestRefund(memberId, orderId, request);

		// then
		assertEquals(expectedResponse, response);

		verify(refundRequestTransactionService).requestRefund(memberId, orderId, request);

		verify(portOneRefundClient).refund(
			"payment-123",
			refundAmount,
			"CHANGE_OF_MIND - 단순 변심"
		);

		// PortOne 환불이 성공했으므로 외부 환불 성공 정보를 먼저 DB에 저장한다.
		verify(refundPortOneSuccessTransactionService).recordPortOneRefundSuccess(
			context,
			refundResult
		);

		// PortOne 성공 정보 저장 이후 내부 환불 완료 처리를 진행한다.
		verify(refundCompleteTransactionService).completeRefund(context);

		// 성공 흐름에서는 실패 처리 트랜잭션이 호출되면 안 된다.
		verify(refundFailTransactionService, never()).failRefund(any(), any());
	}

	@Test
	@DisplayName("환불 상세 사유가 없으면 사유 타입만 PortOne에 전달한다")
	void requestRefundSuccessWithoutReasonDetail() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;
		Long refundAmount = 7000L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.PRODUCT_ISSUE,
			null
		);

		RefundProcessingContext context = createContext(
			RefundReasonType.PRODUCT_ISSUE,
			null
		);

		RefundResponse expectedResponse = new RefundResponse(
			1L,
			orderId,
			20L,
			refundAmount,
			"REFUNDED",
			1,
			10,
			LocalDateTime.now(),
			LocalDateTime.now()
		);

		PortOneRefundResult refundResult = new PortOneRefundResult(
			"cancel-123",
			"SUCCEEDED"
		);

		when(refundRequestTransactionService.requestRefund(memberId, orderId, request))
			.thenReturn(context);

		when(portOneRefundClient.refund(
			eq("payment-123"),
			eq(refundAmount),
			eq("PRODUCT_ISSUE")
		)).thenReturn(refundResult);

		when(refundCompleteTransactionService.completeRefund(context))
			.thenReturn(expectedResponse);

		// when
		RefundResponse response = refundFacade.requestRefund(memberId, orderId, request);

		// then
		assertEquals(expectedResponse, response);

		verify(portOneRefundClient).refund(
			"payment-123",
			refundAmount,
			"PRODUCT_ISSUE"
		);

		// PortOne 환불 성공 정보 저장 후 내부 완료 처리가 호출되는지 검증한다.
		verify(refundPortOneSuccessTransactionService).recordPortOneRefundSuccess(
			context,
			refundResult
		);

		verify(refundCompleteTransactionService).completeRefund(context);

		// 성공 흐름에서는 실패 처리 트랜잭션이 호출되면 안 된다.
		verify(refundFailTransactionService, never()).failRefund(any(), any());
	}

	@Test
	@DisplayName("PortOne 환불 요청 중 예외가 발생하면 환불 실패 처리 후 예외를 던진다")
	void requestRefundFailWhenPortOneThrowsException() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.PAYMENT_ERROR,
			"결제 오류"
		);

		RefundProcessingContext context = createContext(
			RefundReasonType.PAYMENT_ERROR,
			"결제 오류"
		);

		when(refundRequestTransactionService.requestRefund(memberId, orderId, request))
			.thenReturn(context);

		when(portOneRefundClient.refund(
			eq("payment-123"),
			eq(7000L),
			eq("PAYMENT_ERROR - 결제 오류")
		)).thenThrow(new CustomException(ErrorCode.REFUND_FAILED));

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundFacade.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.REFUND_FAILED, exception.getErrorCode());

		verify(refundFailTransactionService).failRefund(
			context,
			"PortOne 환불 요청에 실패했습니다."
		);

		// PortOne이 실패했으므로 PortOne 성공 정보 저장과 내부 완료 처리는 호출되면 안 된다.
		verify(refundPortOneSuccessTransactionService, never())
			.recordPortOneRefundSuccess(any(), any());
		verify(refundCompleteTransactionService, never()).completeRefund(context);
	}

	@Test
	@DisplayName("PortOne 환불 응답이 성공 상태가 아니면 환불 실패 처리 후 예외를 던진다")
	void requestRefundFailWhenPortOneResultIsNotSucceeded() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.ETC,
			"기타 사유"
		);

		RefundProcessingContext context = createContext(
			RefundReasonType.ETC,
			"기타 사유"
		);

		when(refundRequestTransactionService.requestRefund(memberId, orderId, request))
			.thenReturn(context);

		when(portOneRefundClient.refund(
			eq("payment-123"),
			eq(7000L),
			eq("ETC - 기타 사유")
		)).thenReturn(new PortOneRefundResult("cancel-123", "FAILED"));

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundFacade.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.REFUND_FAILED, exception.getErrorCode());

		verify(refundFailTransactionService).failRefund(
			context,
			"PortOne 환불이 성공 상태가 아닙니다."
		);

		// PortOne 환불 결과가 성공이 아니므로 PortOne 성공 정보 저장과 내부 완료 처리는 호출되면 안 된다.
		verify(refundPortOneSuccessTransactionService, never())
			.recordPortOneRefundSuccess(any(), any());
		verify(refundCompleteTransactionService, never()).completeRefund(context);
	}

	/**
	 * 테스트에서 사용할 환불 처리 context를 생성한다.
	 */
	private RefundProcessingContext createContext(
		RefundReasonType reasonType,
		String reasonDetail
	) {
		return new RefundProcessingContext(
			1L,
			10L,
			20L,
			1L,
			"payment-123",
			7000L,
			reasonType,
			reasonDetail
		);
	}
}
