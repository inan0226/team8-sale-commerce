package com.example.team8salecommerce.domain.payment.service;

import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 일반 주문 결제 승인만 담당하는 트랜잭션 서비스다.
 * 기존 특가 결제 승인 서비스와 상태를 분리해 영향 범위를 제한한다.
 */
@Service
@RequiredArgsConstructor
public class NormalOrderPaymentConfirmService {

    private static final String PORT_ONE_PAYMENT_ID_UNIQUE_CONSTRAINT =
            "uk_payment_portone_payment_id";
    private static final String PORT_ONE_PAYMENT_ID_COLUMN = "port_one_payment_id";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 일반 주문을 잠근 뒤 결제 기록을 저장하고 PAID 상태로 변경한다.
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(
            Long memberId,
            Long orderId,
            PortOnePaymentInfo paymentInfo
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validatePayable(order);
        validatePaymentAmount(order, paymentInfo.totalAmount());
        validateDuplicatedPayment(orderId, paymentInfo.paymentId());

        LocalDateTime paidAt = LocalDateTime.now();
        Payment payment = Payment.createPaidPayment(
                orderId,
                PaymentOrderType.NORMAL,
                paymentInfo.paymentId(),
                paymentInfo.totalAmount(),
                null,
                paidAt
        );

        Payment savedPayment = savePayment(payment);
        order.markAsPaid();

        return PaymentConfirmResponse.of(savedPayment, order.getStatus().name());
    }

    /** 결제 가능한 일반 주문 상태인지 검증한다. */
    private void validatePayable(Order order) {
        if (order.isPaid()) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        if (!order.isWaiting()) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    /** 서버 주문 금액과 PortOne 결제 금액이 같은지 검증한다. */
    private void validatePaymentAmount(Order order, Long amount) {
        if (!order.getTotalPrice().equals(amount)) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    /** 일반 주문의 완료 결제와 PortOne 결제 ID 중복을 검증한다. */
    private void validateDuplicatedPayment(Long orderId, String portOnePaymentId) {
        if (paymentRepository.existsByOrderIdAndOrderTypeAndStatus(
                orderId,
                PaymentOrderType.NORMAL,
                PaymentStatus.PAID
        )) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        if (paymentRepository.findByPortOnePaymentId(portOnePaymentId).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
        }
    }

    /** 결제 저장 시 PortOne 고유키 충돌을 도메인 예외로 변환한다. */
    private Payment savePayment(Payment payment) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            if (isPortOnePaymentIdUniqueViolation(exception)) {
                throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
            }
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    /** DB 예외가 PortOne 결제 ID 고유키 위반인지 확인한다. */
    private boolean isPortOnePaymentIdUniqueViolation(DataIntegrityViolationException exception) {
        String message = NestedExceptionUtils.getMostSpecificCause(exception).getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }

        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
        return lowerCaseMessage.contains(PORT_ONE_PAYMENT_ID_UNIQUE_CONSTRAINT)
                || lowerCaseMessage.contains(PORT_ONE_PAYMENT_ID_COLUMN);
    }
}
