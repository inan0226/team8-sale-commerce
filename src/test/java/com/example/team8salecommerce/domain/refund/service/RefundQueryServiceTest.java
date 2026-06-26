package com.example.team8salecommerce.domain.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * RefundQueryService 테스트
 *
 * 환불 상세 조회 기능을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class RefundQueryServiceTest {

	@Mock
	private RefundRepository refundRepository;

	@InjectMocks
	private RefundQueryService refundQueryService;

	@Test
	@DisplayName("본인의 환불 상세 조회에 성공한다")
	void getRefundSuccess() {
		// given
		Long memberId = 1L;
		Long refundId = 10L;
		Long orderId = 100L;
		Long paymentId = 200L;
		Long refundAmount = 7000L;

		Refund refund = createRefund(
			refundId,
			orderId,
			paymentId,
			memberId,
			refundAmount
		);

		when(refundRepository.findByIdAndMemberId(refundId, memberId))
			.thenReturn(Optional.of(refund));

		// when
		RefundResponse response = refundQueryService.getRefund(memberId, refundId);

		// then
		assertEquals(refundId, response.refundId());
		assertEquals(orderId, response.orderId());
		assertEquals(paymentId, response.paymentId());
		assertEquals(refundAmount, response.refundAmount());
		assertEquals("REFUND_REQUEST", response.refundStatus());

		verify(refundRepository).findByIdAndMemberId(refundId, memberId);
	}

	@Test
	@DisplayName("로그인 회원 ID가 없으면 환불 상세 조회에 실패한다")
	void getRefundFailWhenMemberIdIsNull() {
		// given
		Long refundId = 10L;

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundQueryService.getRefund(null, refundId)
		);

		// then
		assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());

		verify(refundRepository, never()).findByIdAndMemberId(null, refundId);
	}

	@Test
	@DisplayName("환불 ID가 없으면 환불 상세 조회에 실패한다")
	void getRefundFailWhenRefundIdIsNull() {
		// given
		Long memberId = 1L;

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundQueryService.getRefund(memberId, null)
		);

		// then
		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(refundRepository, never()).findByIdAndMemberId(memberId, null);
	}

	@Test
	@DisplayName("환불 ID가 0 이하이면 환불 상세 조회에 실패한다")
	void getRefundFailWhenRefundIdIsNotPositive() {
		// given
		Long memberId = 1L;
		Long refundId = 0L;

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundQueryService.getRefund(memberId, refundId)
		);

		// then
		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(refundRepository, never()).findByIdAndMemberId(memberId, refundId);
	}

	@Test
	@DisplayName("본인의 환불 정보가 없으면 환불 상세 조회에 실패한다")
	void getRefundFailWhenRefundNotFound() {
		// given
		Long memberId = 1L;
		Long refundId = 10L;

		when(refundRepository.findByIdAndMemberId(refundId, memberId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundQueryService.getRefund(memberId, refundId)
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_FOUND, exception.getErrorCode());

		verify(refundRepository).findByIdAndMemberId(refundId, memberId);
	}

	/**
	 * 테스트용 환불 요청 엔티티를 생성한다.
	 */
	private Refund createRefund(
		Long refundId,
		Long orderId,
		Long paymentId,
		Long memberId,
		Long refundAmount
	) {
		Refund refund = Refund.createRequest(
			orderId,
			paymentId,
			memberId,
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심",
			refundAmount,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(refund, "id", refundId);

		return refund;
	}
}
