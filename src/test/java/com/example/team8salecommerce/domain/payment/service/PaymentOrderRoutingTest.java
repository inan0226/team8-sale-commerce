package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentClient;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;
import com.example.team8salecommerce.domain.payment.facade.PaymentFailFacade;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 주문 유형에 따라 일반/특가 결제 서비스가 분리 호출되는지 검증한다.
 */
class PaymentOrderRoutingTest {

    @Test
    @DisplayName("NORMAL 결제 승인은 기존 특가 승인 서비스를 호출하지 않는다")
    void routeNormalPaymentConfirmation() {
        // given
        PromotionOrderRepository promotionOrderRepository = mock(PromotionOrderRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        NormalOrderPaymentConfirmService normalService = mock(
                NormalOrderPaymentConfirmService.class
        );
        PortOnePaymentClient portOneClient = mock(PortOnePaymentClient.class);
        PaymentConfirmTransactionService promotionService = mock(
                PaymentConfirmTransactionService.class
        );
        PaymentService paymentService = new PaymentService(
                promotionOrderRepository,
                orderRepository,
                normalService,
                portOneClient,
                promotionService
        );

        Member member = Member.create("normal@example.com", "password", "일반회원");
        Order order = Order.create(member, 20_000L, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 10L);
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                10L,
                "normal-payment-1",
                20_000L,
                PaymentOrderType.NORMAL
        );
        PortOnePaymentInfo info = new PortOnePaymentInfo(
                "normal-payment-1",
                "PAID",
                20_000L
        );
        PaymentConfirmResponse expected = new PaymentConfirmResponse(
                10L,
                100L,
                "normal-payment-1",
                20_000L,
                "PAID",
                "PAID",
                LocalDateTime.now()
        );

        when(orderRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(order));
        when(portOneClient.getPayment("normal-payment-1")).thenReturn(info);
        when(normalService.confirmPayment(1L, 10L, info)).thenReturn(expected);

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(1L, request);

        // then
        assertThat(response).isEqualTo(expected);
        verify(promotionOrderRepository, never()).findByIdAndMemberId(10L, 1L);
        verify(promotionService, never()).confirmPayment(1L, 10L, info);
    }

    @Test
    @DisplayName("NORMAL 결제 실패는 일반 주문 실패 서비스로 위임한다")
    void routeNormalPaymentFailure() {
        // given
        PaymentFailService promotionService = mock(PaymentFailService.class);
        NormalOrderPaymentFailService normalService = mock(NormalOrderPaymentFailService.class);
        PaymentFailFacade facade = new PaymentFailFacade(promotionService, normalService);
        PaymentFailRequest request = new PaymentFailRequest(
                10L,
                "normal-fail-1",
                20_000L,
                "사용자 취소",
                PaymentOrderType.NORMAL
        );
        PaymentFailResponse expected = new PaymentFailResponse(
                10L,
                100L,
                "normal-fail-1",
                20_000L,
                "PAYMENT_FAILED",
                "FAILED",
                LocalDateTime.now(),
                "사용자 취소"
        );

        when(normalService.failPayment(1L, request)).thenReturn(expected);

        // when
        PaymentFailResponse response = facade.failPayment(1L, request);

        // then
        assertThat(response).isEqualTo(expected);
        verify(promotionService, never()).failPayment(1L, request);
    }

    @Test
    @DisplayName("주문 유형이 없으면 기존 특가 주문으로 처리한다")
    void defaultOrderTypeIsPromotion() {
        // when
        PaymentConfirmRequest confirmRequest = new PaymentConfirmRequest(
                10L,
                "payment",
                1_000L
        );
        PaymentFailRequest failRequest = new PaymentFailRequest(
                10L,
                "payment",
                1_000L,
                "실패"
        );

        // then
        assertThat(confirmRequest.orderType()).isEqualTo(PaymentOrderType.PROMOTION);
        assertThat(failRequest.orderType()).isEqualTo(PaymentOrderType.PROMOTION);
    }
}