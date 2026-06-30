package com.example.team8salecommerce.domain.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.team8salecommerce.domain.refund.dto.RefundDetailQuery;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.entity.RefundStatus;
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

		// 환불 상세 조회에 필요한 컬럼만 담은 조회 전용 객체를 준비한다.
		RefundDetailQuery refundDetail = new RefundDetailQuery(
			refundId,
			orderId,
			paymentId,
			refundAmount,
			RefundStatus.REFUND_REQUEST,
			LocalDateTime.now(),
			null
		);

		// 엔티티 전체 조회가 아니라 DTO 직접 조회 메서드가 호출되도록 설정한다.
		when(refundRepository.findDetailByIdAndMemberId(refundId, memberId))
			.thenReturn(Optional.of(refundDetail));

		// when
		RefundResponse response = refundQueryService.getRefund(memberId, refundId);

		// then
		assertEquals(refundId, response.refundId());
		assertEquals(orderId, response.orderId());
		assertEquals(paymentId, response.paymentId());
		assertEquals(refundAmount, response.refundAmount());
		assertEquals("REFUND_REQUEST", response.refundStatus());

		verify(refundRepository).findDetailByIdAndMemberId(refundId, memberId);
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

		// 회원 ID 검증에서 실패했기 때문에 Repository는 호출되면 안 된다.
		verifyNoInteractions(refundRepository);
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

		// 환불 ID 검증에서 실패했기 때문에 Repository는 호출되면 안 된다.
		verifyNoInteractions(refundRepository);
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

		// 환불 ID 검증에서 실패했기 때문에 Repository는 호출되면 안 된다.
		verifyNoInteractions(refundRepository);
	}

	@Test
	@DisplayName("본인의 환불 정보가 없으면 환불 상세 조회에 실패한다")
	void getRefundFailWhenRefundNotFound() {
		// given
		Long memberId = 1L;
		Long refundId = 10L;

		when(refundRepository.findDetailByIdAndMemberId(refundId, memberId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundQueryService.getRefund(memberId, refundId)
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_FOUND, exception.getErrorCode());

		verify(refundRepository).findDetailByIdAndMemberId(refundId, memberId);
	}
}
