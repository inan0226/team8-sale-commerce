package com.example.team8salecommerce.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.facade.PaymentFailFacade;
import com.example.team8salecommerce.domain.payment.service.PaymentService;
import com.example.team8salecommerce.global.security.AuthMember;

/**
 * 결제 Controller 테스트
 *
 * HTTP 요청이 Controller에 정상적으로 들어오고,
 * Controller가 결제 실패 처리 흐름을 PaymentFailFacade에 위임하는지 검증한다.
 */
class PaymentControllerTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long ORDER_ID = 10L;
	private static final Long PAYMENT_ID = 100L;
	private static final String PORT_ONE_PAYMENT_ID = "payment-fail-123";
	private static final Long AMOUNT = 14000L;
	private static final String FAILURE_REASON = "카드 한도 초과";

	private MockMvc mockMvc;
	private PaymentService paymentService;
	private PaymentFailFacade paymentFailFacade;
	private AuthMember authMember;

	@BeforeEach
	void setUp() {
		paymentService = mock(PaymentService.class);
		paymentFailFacade = mock(PaymentFailFacade.class);
		authMember = mock(AuthMember.class);

		// 일부 테스트에서는 인증 객체가 실제로 사용되지 않을 수 있으므로 lenient로 둔다.
		lenient().when(authMember.memberId()).thenReturn(MEMBER_ID);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		PaymentController paymentController = new PaymentController(
			paymentService,
			paymentFailFacade
		);

		mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
			.setCustomArgumentResolvers(createAuthMemberArgumentResolver())
			.setValidator(validator)
			.build();
	}

	@Test
	@DisplayName("결제 실패 처리 API 호출에 성공한다")
	void failPaymentSuccess() throws Exception {
		// given
		String requestBody = """
			{
				"orderId": 10,
				"portOnePaymentId": "payment-fail-123",
				"amount": 14000,
				"failureReason": "카드 한도 초과"
			}
			""";

		PaymentFailResponse response = new PaymentFailResponse(
			ORDER_ID,
			PAYMENT_ID,
			PORT_ONE_PAYMENT_ID,
			AMOUNT,
			"PAYMENT_FAILED",
			"FAILED",
			LocalDateTime.now(),
			FAILURE_REASON
		);

		when(paymentFailFacade.failPayment(eq(MEMBER_ID), any(PaymentFailRequest.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(post("/payments/fail")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("PAYMENT_FAILED")))
			.andExpect(content().string(containsString("FAILED")))
			.andExpect(content().string(containsString(FAILURE_REASON)));

		ArgumentCaptor<PaymentFailRequest> requestCaptor = ArgumentCaptor.forClass(
			PaymentFailRequest.class
		);

		verify(paymentFailFacade).failPayment(eq(MEMBER_ID), requestCaptor.capture());

		PaymentFailRequest capturedRequest = requestCaptor.getValue();

		assertThat(capturedRequest.orderId()).isEqualTo(ORDER_ID);
		assertThat(capturedRequest.portOnePaymentId()).isEqualTo(PORT_ONE_PAYMENT_ID);
		assertThat(capturedRequest.amount()).isEqualTo(AMOUNT);
		assertThat(capturedRequest.failureReason()).isEqualTo(FAILURE_REASON);
	}

	@Test
	@DisplayName("결제 실패 사유가 비어 있으면 결제 실패 처리 API 호출에 실패한다")
	void failPaymentFailWhenFailureReasonIsBlank() throws Exception {
		// given
		String requestBody = """
			{
				"orderId": 10,
				"portOnePaymentId": "payment-fail-123",
				"amount": 14000,
				"failureReason": ""
			}
			""";

		// when & then
		mockMvc.perform(post("/payments/fail")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(paymentFailFacade);
	}

	/**
	 * 테스트용 인증 객체 주입 Resolver
	 *
	 * 실제 서버에서는 Spring Security가 @AuthenticationPrincipal AuthMember를 넣어준다.
	 * 하지만 이 테스트는 Controller만 가볍게 검증하는 standalone 테스트이므로,
	 * 테스트에서 직접 AuthMember를 넣어주는 ArgumentResolver를 등록한다.
	 */
	private HandlerMethodArgumentResolver createAuthMemberArgumentResolver() {
		return new HandlerMethodArgumentResolver() {

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
					&& AuthMember.class.isAssignableFrom(parameter.getParameterType());
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

	@Test
	@DisplayName("주문 ID가 0이면 결제 실패 처리 API 호출에 실패한다")
	void failPaymentFailWhenOrderIdIsZero() throws Exception {
		// given
		String requestBody = """
		{
			"orderId": 0,
			"portOnePaymentId": "payment-fail-123",
			"amount": 14000,
			"failureReason": "카드 한도 초과"
		}
		""";

		// when & then
		mockMvc.perform(post("/payments/fail")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(paymentFailFacade);
	}

	@Test
	@DisplayName("결제 실패 사유가 255자를 초과하면 결제 실패 처리 API 호출에 실패한다")
	void failPaymentFailWhenFailureReasonIsTooLong() throws Exception {
		// given
		String longFailureReason = "a".repeat(256);

		String requestBody = """
		{
			"orderId": 10,
			"portOnePaymentId": "payment-fail-123",
			"amount": 14000,
			"failureReason": "%s"
		}
		""".formatted(longFailureReason);

		// when & then
		mockMvc.perform(post("/payments/fail")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(paymentFailFacade);
	}
}
