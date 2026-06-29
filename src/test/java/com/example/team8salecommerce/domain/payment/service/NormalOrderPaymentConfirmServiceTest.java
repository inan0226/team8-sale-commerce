package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 일반 주문 결제 승인 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class NormalOrderPaymentConfirmServiceTest {

    @InjectMocks
    private NormalOrderPaymentConfirmService service;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("일반 주문 결제를 승인하면 NORMAL 결제가 저장되고 주문이 PAID가 된다")
    void confirmNormalOrderPayment() {
        // given
        Member member = Member.create("normal@example.com", "password", "일반회원");
        ReflectionTestUtils.setField(member, "id", 1L);
        Order order = Order.create(member, 20_000L, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 10L);
        PortOnePaymentInfo paymentInfo = new PortOnePaymentInfo(
                "normal-payment-1",
                "PAID",
                20_000L
        );

        when(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findByPortOnePaymentId("normal-payment-1"))
                .thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(payment, "id", 100L);
                    return payment;
                });

        // when
        PaymentConfirmResponse response = service.confirmPayment(1L, 10L, paymentInfo);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.paymentId()).isEqualTo(100L);
        assertThat(response.orderStatus()).isEqualTo("PAID");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        org.mockito.Mockito.verify(paymentRepository).saveAndFlush(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getOrderType()).isEqualTo(PaymentOrderType.NORMAL);
    }
}
