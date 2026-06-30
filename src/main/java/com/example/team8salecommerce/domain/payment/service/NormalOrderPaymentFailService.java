package com.example.team8salecommerce.domain.payment.service;

import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.entity.OrderItem;
import com.example.team8salecommerce.domain.order.repository.OrderItemRepository;
import com.example.team8salecommerce.domain.order.repository.OrderProductRepository;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentOrderType;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 일반 주문 결제 실패와 상품 재고 복구만 담당한다.
 * 기존 특가 결제 실패 서비스는 그대로 유지한다.
 */
@Service
@RequiredArgsConstructor
public class NormalOrderPaymentFailService {

    private static final String PORT_ONE_PAYMENT_ID_UNIQUE_CONSTRAINT =
            "uk_payment_portone_payment_id";
    private static final String PORT_ONE_PAYMENT_ID_COLUMN = "port_one_payment_id";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderProductRepository orderProductRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 일반 주문 결제 실패 기록, 주문 상태 변경, 재고 복구를 한 트랜잭션에서 처리한다.
     */
    @Transactional
    public PaymentFailResponse failPayment(Long memberId, PaymentFailRequest request) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(
                request.orderId(),
                memberId
        ).orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validateFailRequest(order, request);
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        Map<Long, Product> lockedProducts = lockProducts(orderItems);

        LocalDateTime failedAt = LocalDateTime.now();
        Payment payment = Payment.createFailedPayment(
                order.getId(),
                PaymentOrderType.NORMAL,
                request.portOnePaymentId(),
                request.amount(),
                null,
                failedAt,
                request.failureReason()
        );

        Payment savedPayment = savePayment(payment);
        order.failPayment();
        restoreStock(orderItems, lockedProducts);

        return PaymentFailResponse.of(savedPayment, order.getStatus().name());
    }

    /** 실패 처리 가능한 주문 상태, 금액, 결제 중복을 검증한다. */
    private void validateFailRequest(Order order, PaymentFailRequest request) {
        if (!order.isWaiting()) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (!order.getTotalPrice().equals(request.amount())) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (paymentRepository.existsByOrderIdAndOrderTypeAndStatus(
                order.getId(),
                PaymentOrderType.NORMAL,
                PaymentStatus.PAID
        )) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        if (paymentRepository.existsByOrderIdAndOrderTypeAndStatus(
                order.getId(),
                PaymentOrderType.NORMAL,
                PaymentStatus.FAILED
        )) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_FAILED);
        }
        if (paymentRepository.findByPortOnePaymentId(request.portOnePaymentId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
        }
    }

    /** 일반 주문 상품의 원본 상품을 ID 순서대로 잠근다. */
    private Map<Long, Product> lockProducts(List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }

        List<Long> productIds = orderItems.stream()
                .map(orderItem -> orderItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        List<Product> products = orderProductRepository.findAllByIdInForUpdate(productIds);
        if (products.size() != productIds.size()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    /** 모든 일반 주문 상품 수량만큼 재고를 복구한다. */
    private void restoreStock(List<OrderItem> orderItems, Map<Long, Product> products) {
        for (OrderItem orderItem : orderItems) {
            Product product = products.get(orderItem.getProduct().getId());
            int restoredRows = orderProductRepository.restoreStock(
                    product.getId(),
                    orderItem.getQuantity()
            );
            if (restoredRows == 0) {
                throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
            }
        }
    }

    /** 실패 결제 저장 시 PortOne 고유키 충돌을 도메인 예외로 변환한다. */
    private Payment savePayment(Payment payment) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            if (isPortOnePaymentIdUniqueViolation(exception)) {
                throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
            }
            throw new CustomException(ErrorCode.PAYMENT_FAIL_FAILED);
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
