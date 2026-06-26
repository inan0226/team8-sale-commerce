package com.example.team8salecommerce.domain.refund.client;

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
 * PortOne REST 환불 Client
 *
 * PortOne V2 결제 취소 API를 호출해서
 * 실제 결제 취소, 즉 환불을 요청한다.
 */
@Component
public class PortOneRestRefundClient implements PortOneRefundClient {

	private static final String PORTONE_AUTHORIZATION_PREFIX = "PortOne ";
	private static final String PORTONE_BASE_URL = "api.portone.io";
	private static final String SUCCEEDED_STATUS = "SUCCEEDED";

	private final RestClient restClient;
	private final String apiSecret;
	private final String storeId;

	public PortOneRestRefundClient(
		@Value("${portone.api.secret:}") String apiSecret,
		@Value("${portone.store-id:}") String storeId,
		@Value("${portone.api.connect-timeout-millis:3000}") int connectTimeoutMillis,
		@Value("${portone.api.read-timeout-millis:5000}") int readTimeoutMillis
	) {
		this.restClient = RestClient.builder()
			.baseUrl(PORTONE_BASE_URL)
			.requestFactory(createRequestFactory(connectTimeoutMillis, readTimeoutMillis))
			.build();
		this.apiSecret = apiSecret;
		this.storeId = storeId;
	}

	/**
	 * PortOne 결제 취소 API를 호출한다.
	 *
	 * 결제 조회 Client와 동일하게 API Secret이 없거나
	 * PortOne API 호출에 실패하면 REFUND_FAILED 예외로 변환한다.
	 */
	@Override
	public PortOneRefundResult refund(
		String portOnePaymentId,
		Long amount,
		String reason
	) {
		validateApiSecret();
		validateRefundRequest(portOnePaymentId, amount);

		try {
			PortOneCancelPaymentResponse response = restClient.post()
				.uri("/payments/{paymentId}/cancel", portOnePaymentId)
				.header(HttpHeaders.AUTHORIZATION, PORTONE_AUTHORIZATION_PREFIX + apiSecret)
				.body(createRequest(amount, reason))
				.retrieve()
				.body(PortOneCancelPaymentResponse.class);

			if (response == null || response.cancellation() == null) {
				throw new CustomException(ErrorCode.REFUND_FAILED);
			}

			PortOnePaymentCancellation cancellation = response.cancellation();

			return new PortOneRefundResult(
				cancellation.id(),
				cancellation.status()
			);
		} catch (RestClientException e) {
			throw new CustomException(ErrorCode.REFUND_FAILED);
		}
	}

	/**
	 * PortOne 환불 요청 Body를 생성한다.
	 *
	 * storeId는 프로젝트/PortOne 설정에 따라 필요할 수 있으므로
	 * 설정값이 있으면 함께 전달하고, 없으면 null로 둔다.
	 */
	private PortOneCancelPaymentRequest createRequest(Long amount, String reason) {
		String requestStoreId = null;

		if (StringUtils.hasText(storeId)) {
			requestStoreId = storeId;
		}

		return new PortOneCancelPaymentRequest(
			requestStoreId,
			amount,
			reason
		);
	}

	/**
	 * PortOne API 호출용 RequestFactory를 생성한다.
	 *
	 * 외부 API는 네트워크 지연이나 장애가 발생할 수 있으므로
	 * timeout을 명시해 요청이 무한정 대기하지 않도록 한다.
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
	 */
	private void validateApiSecret() {
		if (!StringUtils.hasText(apiSecret)) {
			throw new CustomException(ErrorCode.REFUND_FAILED);
		}
	}

	/**
	 * PortOne 환불 요청에 필요한 값을 검증한다.
	 */
	private void validateRefundRequest(String portOnePaymentId, Long amount) {
		if (!StringUtils.hasText(portOnePaymentId)) {
			throw new CustomException(ErrorCode.REFUND_FAILED);
		}

		if (amount == null || amount <= 0) {
			throw new CustomException(ErrorCode.REFUND_FAILED);
		}
	}

	/**
	 * PortOne 결제 취소 요청 Body
	 */
	private record PortOneCancelPaymentRequest(
		String storeId,
		Long amount,
		String reason
	) {
	}

	/**
	 * PortOne 결제 취소 응답
	 */
	private record PortOneCancelPaymentResponse(
		PortOnePaymentCancellation cancellation
	) {
	}

	/**
	 * PortOne 결제 취소 정보
	 */
	private record PortOnePaymentCancellation(
		String id,
		String status
	) {
	}
}
