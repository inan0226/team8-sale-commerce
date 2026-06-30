package com.example.team8salecommerce.domain.payment.facade;

import org.springframework.stereotype.Component;

import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;
import com.example.team8salecommerce.domain.payment.service.PaymentFailService;
import com.example.team8salecommerce.domain.payment.service.NormalOrderPaymentFailService;

import lombok.RequiredArgsConstructor;

/**
 * 결제 실패 처리 Facade
 *
 * Controller가 결제 실패 처리의 세부 흐름을 알지 않아도 되도록
 * 결제 실패 처리 진입점을 담당한다.
 *
 * 현재는 PaymentFailService에 위임하는 얇은 Facade지만,
 * 추후 PortOne 실패 내역 조회, Redis Lock, 알림, 이벤트 발행 등이 추가되어도
 * Controller 코드는 변경하지 않고 이 Facade에서 흐름을 조합할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class PaymentFailFacade {

	private final PaymentFailService paymentFailService;
	private final NormalOrderPaymentFailService normalOrderPaymentFailService;

	/**
	 * 결제 실패 처리
	 *
	 * 인증된 회원 ID와 결제 실패 요청 정보를 받아
	 * 실제 결제 실패 처리 Service로 위임한다.
	 */
	public PaymentFailResponse failPayment(Long memberId, PaymentFailRequest request) {
		if (request.orderType() == PaymentOrderType.NORMAL) {
			return normalOrderPaymentFailService.failPayment(memberId, request);
		}

		return paymentFailService.failPayment(memberId, request);
	}
}
