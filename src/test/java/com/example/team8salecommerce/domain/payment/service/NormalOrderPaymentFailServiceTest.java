package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.entity.OrderItem;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;
import com.example.team8salecommerce.domain.order.repository.OrderItemRepository;
import com.example.team8salecommerce.domain.order.repository.OrderProductRepository;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 일반 주문 결제 실패와 재고 복구 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class NormalOrderPaymentFailServiceTest {

    @InjectMocks
    private NormalOrderPaymentFailService service;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderProductRepository orderProductRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("일반 주문 결제 실패 시 NORMAL 결제를 저장하고 상품 재고를 복구한다")
    void failNormalOrderPaymentAndRestoreStock() {
        // given
        Member member = Member.create("normal@example.com", "password", "일반회원");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = Product.create(
                "키보드",
                "브랜드",
                10_000L,
                3,
                "image.png",
                "설명",
                Mockito.mock(Category.class)
        );
        ReflectionTestUtils.setField(product, "id", 20L);
        Order order = Order.create(member, 20_000L, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 10L);
        OrderItem orderItem = OrderItem.create(order, product, 2);
        PaymentFailRequest request = new PaymentFailRequest(
                10L,
                "normal-fail-1",
                20_000L,
                "사용자 취소",
                PaymentOrderType.NORMAL
        );

        when(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findByPortOnePaymentId("normal-fail-1"))
                .thenReturn(Optional.empty());
        when(orderItemRepository.findAllByOrderId(10L)).thenReturn(List.of(orderItem));
        when(orderProductRepository.findAllByIdInForUpdate(List.of(20L)))
                .thenReturn(List.of(product));
        when(orderProductRepository.restoreStock(20L, 2)).thenReturn(1);
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(payment, "id", 100L);
                    return payment;
                });

        // when
        PaymentFailResponse response = service.failPayment(1L, request);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(response.orderStatus()).isEqualTo("PAYMENT_FAILED");
        verify(orderProductRepository).restoreStock(20L, 2);
    }
}