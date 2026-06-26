package com.example.team8salecommerce.domain.refund.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.team8salecommerce.domain.refund.dto.RefundRequest;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.facade.RefundFacade;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.security.AuthMember;

/**
 * RefundController 테스트
 *
 * 환불 요청 API의 HTTP 요청/응답과
 * 요청 값 검증이 정상 동작하는지 확인한다.
 */
class RefundControllerTest {

	private MockMvc mockMvc;
	private RefundFacade refundFacade;
	private AuthMember authMember;

	@BeforeEach
	void setUp() {
		refundFacade = mock(RefundFacade.class);
		authMember = mock(AuthMember.class);

		when(authMember.memberId()).thenReturn(1L);

		RefundController refundController = new RefundController(refundFacade);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(refundController)
			.setValidator(validator)
			.setControllerAdvice(new TestExceptionHandler())
			.setCustomArgumentResolvers(createAuthenticationPrincipalResolver())
			.build();
	}

	@Test
	@DisplayName("환불 요청 API 호출에 성공한다")
	void requestRefundSuccess() throws Exception {
		// given
		Long orderId = 10L;

		String requestBody = """
			{
				"reasonType": "CHANGE_OF_MIND",
				"reasonDetail": "단순 변심"
			}
			""";

		RefundResponse response = new RefundResponse(
			1L,
			orderId,
			20L,
			7000L,
			"REFUNDED",
			1,
			10,
			LocalDateTime.now(),
			LocalDateTime.now()
		);

		when(refundFacade.requestRefund(eq(1L), eq(orderId), any(RefundRequest.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"refundId\":1")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"orderId\":10")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"paymentId\":20")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"refundAmount\":7000")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"refundStatus\":\"REFUNDED\"")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"restoredEventStock\":1")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"remainingEventStock\":10")));

		verify(refundFacade).requestRefund(
			eq(1L),
			eq(orderId),
			any(RefundRequest.class)
		);
	}

	@Test
	@DisplayName("주문 ID가 0이면 환불 요청 API 호출에 실패한다")
	void requestRefundFailWhenOrderIdIsZero() throws Exception {
		// given
		String requestBody = """
			{
				"reasonType": "CHANGE_OF_MIND",
				"reasonDetail": "단순 변심"
			}
			""";

		// when & then
		mockMvc.perform(post("/orders/{orderId}/refunds", 0)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(refundFacade);
	}

	@Test
	@DisplayName("환불 사유 타입이 없으면 환불 요청 API 호출에 실패한다")
	void requestRefundFailWhenReasonTypeIsNull() throws Exception {
		// given
		String requestBody = """
			{
				"reasonDetail": "단순 변심"
			}
			""";

		// when & then
		mockMvc.perform(post("/orders/{orderId}/refunds", 10)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(refundFacade);
	}

	@Test
	@DisplayName("환불 상세 사유가 500자를 초과하면 환불 요청 API 호출에 실패한다")
	void requestRefundFailWhenReasonDetailIsTooLong() throws Exception {
		// given
		String longReasonDetail = "a".repeat(501);

		String requestBody = """
			{
				"reasonType": "CHANGE_OF_MIND",
				"reasonDetail": "%s"
			}
			""".formatted(longReasonDetail);

		// when & then
		mockMvc.perform(post("/orders/{orderId}/refunds", 10)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(refundFacade);
	}

	/**
	 * @AuthenticationPrincipal AuthMember를 테스트에서 직접 주입하기 위한 Resolver
	 *
	 * 실제 요청에서는 Spring Security가 인증 정보를 넣어주지만,
	 * standalone MockMvc 테스트에서는 직접 AuthMember를 반환해줘야 한다.
	 */
	private HandlerMethodArgumentResolver createAuthenticationPrincipalResolver() {
		return new HandlerMethodArgumentResolver() {

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
					&& parameter.getParameterType().equals(AuthMember.class);
			}

			@Override
			public Object resolveArgument(
				MethodParameter parameter,
				ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest,
				WebDataBinderFactory binderFactory
			) {
				return authMember;
			}
		};
	}

	/**
	 * standalone MockMvc 테스트용 예외 핸들러
	 *
	 * 실제 애플리케이션에서는 전역 예외 핸들러가 CustomException을 처리하지만,
	 * standalone MockMvc 테스트에서는 직접 등록해줘야 한다.
	 */
	@RestControllerAdvice
	private static class TestExceptionHandler {

		@ExceptionHandler(CustomException.class)
		public ResponseEntity<Void> handleCustomException(CustomException exception) {
			return ResponseEntity
				.status(exception.getErrorCode().getStatus())
				.build();
		}
	}
}
