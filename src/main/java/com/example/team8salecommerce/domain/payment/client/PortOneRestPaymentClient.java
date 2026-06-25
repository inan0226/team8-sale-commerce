package com.example.team8salecommerce.domain.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * PortOne REST API Client
 *
 * PortOne V2 결제 단건 조회 API를 호출해서
 * 실제 결제 상태와 결제 금액을 조회한다.
 */
@Component
public class PortOneRestPaymentClient implements PortOnePaymentClient {

	private static final String PORTONE_AUTHORIZATION_PREFIX = "PortOne ";
	private static final String PORTONE_BASE_URL = "https://api.portone.io";

	private final RestClient restClient;
	private final String apiSecret;

	public PortOneRestPaymentClient(
		@Value("${portone.api.secret:}") String apiSecret,
		@Value("${portone.api.connect-timeout-millis:3000}") int connectTimeoutMillis,
		@Value("${portone.api.read-timeout-millis:5000}") int readTimeoutMillis
	) {
		this.restClient = RestClient.builder()
			.baseUrl(PORTONE_BASE_URL)
			.requestFactory(createRequestFactory(connectTimeoutMillis, readTimeoutMillis))
			.build();
		this.apiSecret = apiSecret;
	}

	@Override
	public PortOnePaymentInfo getPayment(String portOnePaymentId) {
		validateApiSecret();

		try {
			PortOnePaymentResponse response = restClient.get()
				.uri("/payments/{paymentId}", portOnePaymentId)
				.header(HttpHeaders.AUTHORIZATION, PORTONE_AUTHORIZATION_PREFIX + apiSecret)
				.retrieve()
				.body(PortOnePaymentResponse.class);

			if (response == null || response.amount() == null) {
				throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
			}

			return new PortOnePaymentInfo(
				response.id(),
				response.status(),
				response.amount().total()
			);
		} catch (RestClientException e) {
			throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		}
	}

	/**
	 * PortOne API 호출용 RequestFactory를 생성한다.
	 *
	 * 외부 API는 네트워크 지연이나 장애가 발생할 수 있으므로
	 * connect timeout과 read timeout을 명시해서
	 * 요청이 무한정 대기하지 않도록 방어한다.
	 */
	private SimpleClientHttpRequestFactory createRequestFactory(
		int connectTimeoutMillis,
		int readTimeoutMillis
	) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

		requestFactory.setConnectTimeout(connectTimeoutMillis);
		requestFactory.setReadTimeout(readTimeoutMillis);

		return requestFactory;
	}

	/**
	 * PortOne API Secret 설정 여부를 검증한다.
	 *
	 * 실제 PortOne 서버 검증을 위해서는 API Secret이 필요하다.
	 */
	private void validateApiSecret() {
		if (!StringUtils.hasText(apiSecret)) {
			throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		}
	}

	/**
	 * PortOne 결제 단건 조회 응답 중
	 * 현재 서비스에서 필요한 필드만 매핑한다.
	 */
	private record PortOnePaymentResponse(
		String id,
		String status,
		PortOnePaymentAmount amount
	) {
	}

	/**
	 * PortOne 결제 금액 정보
	 */
	private record PortOnePaymentAmount(
		Long total
	) {
	}
}
