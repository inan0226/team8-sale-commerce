package com.example.team8salecommerce.domain.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.team8salecommerce.domain.order.controller.OrderController;
import com.example.team8salecommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.team8salecommerce.domain.order.dto.response.OrderListResponse;
import com.example.team8salecommerce.domain.order.dto.response.OrderResponse;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;
import com.example.team8salecommerce.domain.order.service.OrderService;
import com.example.team8salecommerce.global.exception.GlobalExceptionHandler;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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


class OrderControllerTest {

	private static final Long MEMBER_ID = 1L;

	private static final Long ORDER_ID = 100L;

	private MockMvc mockMvc;

	private OrderService orderService;

	private AuthMember authMember;

	@BeforeEach
	void setUp() {
		orderService = mock(OrderService.class);
		authMember = mock(AuthMember.class);
		when(authMember.memberId()).thenReturn(MEMBER_ID);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, new AuthMemberResolver()))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(createAuthMemberArgumentResolver())
			.setValidator(validator)
			.build();
	}

	@Test
	@DisplayName("POST /orders 요청으로 주문을 생성한다")
	void createOrderSuccess() throws Exception {
		// given: 주문 서비스가 결제 대기 주문 응답을 반환하도록 준비
		when(orderService.createOrder(any(Long.class), any(CreateOrderRequest.class)))
			.thenReturn(createOrderResponse(OrderStatus.WAITING));

		// when & then: 생성 상태 코드와 주문 상태를 확인
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"cartItemIds":[10,20]}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
			.andExpect(jsonPath("$.data.status").value("WAITING"));

		verify(orderService).createOrder(any(Long.class), any(CreateOrderRequest.class));
	}

	@Test
	@DisplayName("GET /orders 요청으로 회원 주문 목록을 조회한다")
	void getOrdersSuccess() throws Exception {
		// given: 주문 한 건이 포함된 목록 응답을 준비
		when(orderService.getOrders(MEMBER_ID))
			.thenReturn(new OrderListResponse(List.of(createOrderResponse(OrderStatus.WAITING))));

		// when & then: 주문 목록과 식별자를 확인
		mockMvc.perform(get("/orders"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orders.length()").value(1))
			.andExpect(jsonPath("$.data.orders[0].orderId").value(ORDER_ID));

		verify(orderService).getOrders(MEMBER_ID);
	}

	@Test
	@DisplayName("PATCH /orders/{orderId}/cancel 요청으로 주문을 취소한다")
	void cancelOrderSuccess() throws Exception {
		// given: 주문 서비스가 취소 상태 응답을 반환하도록 준비
		when(orderService.cancelOrder(MEMBER_ID, ORDER_ID))
			.thenReturn(createOrderResponse(OrderStatus.CANCELLED));

		// when & then: 취소 상태와 성공 응답을 확인
		mockMvc.perform(patch("/orders/{orderId}/cancel", ORDER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("CANCELLED"));

		verify(orderService).cancelOrder(MEMBER_ID, ORDER_ID);
	}

	@Test
	@DisplayName("주문할 장바구니 상품이 없으면 400 응답을 반환한다")
	void createOrderFailsWhenCartItemIdsAreEmpty() throws Exception {
		// when & then: DTO 검증 실패와 서비스 미호출을 확인
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"cartItemIds":[]}
					"""))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(orderService);
	}

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

	private OrderResponse createOrderResponse(OrderStatus status) {
		return new OrderResponse(
			ORDER_ID,
			20_000L,
			status,
			LocalDateTime.of(2026, 6, 29, 12, 0),
			List.of()
		);
	}
}
