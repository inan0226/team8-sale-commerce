package com.example.team8salecommerce.domain.payment.client;

/**
 * PortOne 결제 정보 조회 Client
 *
 * 서버가 클라이언트에서 전달받은 결제 ID를 그대로 신뢰하지 않고,
 * PortOne 서버에 실제 결제 정보를 조회하기 위한 역할을 담당한다.
 */
public interface PortOnePaymentClient {

	/**
	 * PortOne 결제 ID로 실제 결제 정보를 조회한다.
	 */
	PortOnePaymentInfo getPayment(String portOnePaymentId);
}
