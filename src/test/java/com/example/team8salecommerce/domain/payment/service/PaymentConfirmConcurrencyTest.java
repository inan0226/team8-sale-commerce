package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.team8salecommerce.domain.payment.client.PortOnePaymentClient;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * 결제 승인 동시성 테스트
 *
 * 같은 주문에 서로 다른 PortOne 결제 ID로 동시에 승인 요청이 들어와도
 * 주문 row lock으로 인해 하나의 요청만 결제 완료되는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentConfirmConcurrencyTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long PROMOTION_PRODUCT_ID = 100L;
	private static final Long ORDER_AMOUNT = 14000L;
	private static final int REQUEST_COUNT = 2;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PromotionOrderRepository promotionOrderRepository;

	@MockitoBean
	private PortOnePaymentClient portOnePaymentClient;

	@MockitoBean
	private RedissonClient redissonClient;

	@BeforeEach
	void setUp() {
		paymentRepository.deleteAll();
		promotionOrderRepository.deleteAll();
	}

	@Test
	@DisplayName("같은 주문에 서로 다른 PortOne 결제 ID로 동시에 승인 요청해도 하나만 성공한다")
	void confirmPaymentConcurrencySuccessOnlyOnce() throws InterruptedException {
		// given
		PromotionOrder promotionOrder = promotionOrderRepository.saveAndFlush(
			PromotionOrder.create(
				MEMBER_ID,
				PROMOTION_PRODUCT_ID,
				ORDER_AMOUNT,
				LocalDateTime.now()
			)
		);

		Long orderId = promotionOrder.getId();

		when(portOnePaymentClient.getPayment(anyString()))
			.thenAnswer(invocation -> {
				String portOnePaymentId = invocation.getArgument(0);

				return new PortOnePaymentInfo(
					portOnePaymentId,
					"PAID",
					ORDER_AMOUNT
				);
			});

		ExecutorService executorService = Executors.newFixedThreadPool(REQUEST_COUNT);
		CountDownLatch readyLatch = new CountDownLatch(REQUEST_COUNT);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(REQUEST_COUNT);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger alreadyCompletedFailCount = new AtomicInteger();
		List<Throwable> unexpectedExceptions = new CopyOnWriteArrayList<>();

		List<String> portOnePaymentIds = List.of(
			"payment-concurrency-1",
			"payment-concurrency-2"
		);

		for (String portOnePaymentId : portOnePaymentIds) {
			executorService.submit(() -> {
				try {
					readyLatch.countDown();
					startLatch.await();

					PaymentConfirmRequest request = new PaymentConfirmRequest(
						orderId,
						portOnePaymentId,
						ORDER_AMOUNT
					);

					paymentService.confirmPayment(MEMBER_ID, request);

					successCount.incrementAndGet();
				} catch (CustomException e) {
					if (e.getErrorCode() == ErrorCode.PAYMENT_ALREADY_COMPLETED) {
						alreadyCompletedFailCount.incrementAndGet();
					} else {
						unexpectedExceptions.add(e);
					}
				} catch (Throwable e) {
					unexpectedExceptions.add(e);
				} finally {
					doneLatch.countDown();
				}
			});
		}

		assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();

		startLatch.countDown();

		assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();

		executorService.shutdownNow();

		// then
		assertThat(unexpectedExceptions).isEmpty();

		// 같은 주문에 대한 동시 결제 승인 요청 중 하나만 성공해야 한다.
		assertThat(successCount.get()).isEqualTo(1);

		// 나머지 요청은 이미 결제 완료된 주문으로 실패해야 한다.
		assertThat(alreadyCompletedFailCount.get()).isEqualTo(1);

		List<Payment> payments = paymentRepository.findAll();

		// Payment도 성공 요청 하나에 대해서만 저장되어야 한다.
		assertThat(payments).hasSize(1);
		assertThat(payments.get(0).getOrderId()).isEqualTo(orderId);
		assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.PAID);

		PromotionOrder savedPromotionOrder = promotionOrderRepository.findById(orderId)
			.orElseThrow();

		// 주문은 최종적으로 PAID 상태여야 한다.
		assertThat(savedPromotionOrder.isPaid()).isTrue();
	}
}
