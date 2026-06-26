package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.transaction.support.TransactionTemplate;

import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderStatus;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.domain.stock.entity.StockChangeReason;
import com.example.team8salecommerce.domain.stock.entity.StockChangeType;
import com.example.team8salecommerce.domain.stock.entity.StockHistory;
import com.example.team8salecommerce.domain.stock.repository.StockHistoryRepository;

import jakarta.persistence.EntityManager;

/**
 * 결제 실패 처리 동시성 테스트
 *
 * 서로 다른 주문이 같은 특가 상품의 이벤트 재고를 동시에 복구해도
 * PromotionProduct row lock으로 인해 lost update 없이 재고가 정확히 복구되는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentFailConcurrencyTest {

	private static final Long MEMBER_ID_1 = 1L;
	private static final Long MEMBER_ID_2 = 2L;
	private static final Long PRODUCT_ID = 100L;
	private static final Long ORDER_AMOUNT = 7000L;
	private static final Long UNIT_PRICE = 7000L;
	private static final int TOTAL_EVENT_STOCK = 10;
	private static final int PURCHASE_QUANTITY = 1;
	private static final int REQUEST_COUNT = 2;
	private static final String FAILURE_REASON = "결제 실패 동시성 테스트";

	@Autowired
	private PaymentFailService paymentFailService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PromotionOrderRepository promotionOrderRepository;

	@Autowired
	private PromotionOrderItemRepository promotionOrderItemRepository;

	@Autowired
	private PromotionProductRepository promotionProductRepository;

	@Autowired
	private StockHistoryRepository stockHistoryRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private EntityManager entityManager;

	/**
	 * 이 테스트는 Redis Lock 동작을 검증하는 테스트가 아니다.
	 * Spring Context 로딩 시 RedissonClient 실제 연결을 피하기 위해 Mock 처리한다.
	 */
	@MockitoBean
	private RedissonClient redissonClient;

	@BeforeEach
	void setUp() {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from StockHistory").executeUpdate();
			entityManager.createQuery("delete from Payment").executeUpdate();
			entityManager.createQuery("delete from PromotionOrderItem").executeUpdate();
			entityManager.createQuery("delete from PromotionOrder").executeUpdate();
			entityManager.createQuery("delete from PromotionProduct").executeUpdate();
		});
	}

	@Test
	@DisplayName("서로 다른 주문이 같은 특가 상품 재고를 동시에 복구해도 재고가 정확히 복구된다")
	void failPaymentConcurrentlyRestoreStockCorrectly() throws InterruptedException {
		// given
		TestFixture fixture = createTestFixture();

		ExecutorService executorService = Executors.newFixedThreadPool(REQUEST_COUNT);

		// 모든 스레드가 준비될 때까지 기다리는 용도다.
		CountDownLatch readyLatch = new CountDownLatch(REQUEST_COUNT);

		// 모든 스레드가 거의 동시에 결제 실패 처리를 시작하게 만드는 용도다.
		CountDownLatch startLatch = new CountDownLatch(1);

		// 모든 스레드의 결제 실패 처리가 끝났는지 확인하는 용도다.
		CountDownLatch doneLatch = new CountDownLatch(REQUEST_COUNT);

		AtomicInteger successCount = new AtomicInteger();

		// 예상하지 못한 예외가 발생하면 테스트 마지막에 확인하기 위해 모아둔다.
		List<Throwable> unexpectedExceptions = new CopyOnWriteArrayList<>();

		List<FailCommand> failCommands = List.of(
			new FailCommand(
				MEMBER_ID_1,
				fixture.orderId1(),
				"payment-fail-concurrency-1"
			),
			new FailCommand(
				MEMBER_ID_2,
				fixture.orderId2(),
				"payment-fail-concurrency-2"
			)
		);

		// when
		for (FailCommand failCommand : failCommands) {
			executorService.submit(() -> {
				try {
					readyLatch.countDown();
					startLatch.await();

					PaymentFailRequest request = new PaymentFailRequest(
						failCommand.orderId(),
						failCommand.portOnePaymentId(),
						ORDER_AMOUNT,
						FAILURE_REASON
					);

					paymentFailService.failPayment(failCommand.memberId(), request);
					successCount.incrementAndGet();
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

		// 서로 다른 주문 2건은 모두 결제 실패 처리에 성공해야 한다.
		assertThat(successCount.get()).isEqualTo(REQUEST_COUNT);

		PromotionProduct promotionProduct = promotionProductRepository
			.findById(fixture.promotionProductId())
			.orElseThrow();

		// 처음 10개 중 WAITING 주문 2건으로 2개가 차감되어 8개였고,
		// 결제 실패 2건으로 2개가 복구되어 다시 10개가 되어야 한다.
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(TOTAL_EVENT_STOCK);

		PromotionOrder order1 = promotionOrderRepository.findById(fixture.orderId1())
			.orElseThrow();
		PromotionOrder order2 = promotionOrderRepository.findById(fixture.orderId2())
			.orElseThrow();

		assertThat(order1.getStatus()).isEqualTo(PromotionOrderStatus.PAYMENT_FAILED);
		assertThat(order2.getStatus()).isEqualTo(PromotionOrderStatus.PAYMENT_FAILED);
		assertThat(order1.getPaymentFailedAt()).isNotNull();
		assertThat(order2.getPaymentFailedAt()).isNotNull();

		List<Payment> payments = paymentRepository.findAll();

		assertThat(payments).hasSize(REQUEST_COUNT);
		assertThat(payments)
			.extracting(Payment::getStatus)
			.containsOnly(PaymentStatus.FAILED);
		assertThat(payments)
			.extracting(Payment::getPortOnePaymentId)
			.containsExactlyInAnyOrder(
				"payment-fail-concurrency-1",
				"payment-fail-concurrency-2"
			);

		List<StockHistory> stockHistories = stockHistoryRepository.findAll();

		assertThat(stockHistories).hasSize(REQUEST_COUNT);
		assertThat(stockHistories)
			.extracting(StockHistory::getType)
			.containsOnly(StockChangeType.RESTORE);
		assertThat(stockHistories)
			.extracting(StockHistory::getReason)
			.containsOnly(StockChangeReason.PAYMENT_FAILED);
		assertThat(stockHistories)
			.extracting(StockHistory::getQuantity)
			.containsOnly(PURCHASE_QUANTITY);

		// row lock이 정상 동작하면 복구 이력은 8 -> 9, 9 -> 10 순서로 남아야 한다.
		assertThat(stockHistories)
			.extracting(StockHistory::getStockBefore)
			.containsExactlyInAnyOrder(8, 9);
		assertThat(stockHistories)
			.extracting(StockHistory::getStockAfter)
			.containsExactlyInAnyOrder(9, 10);
	}

	/**
	 * 동시성 테스트에 필요한 특가 상품, 주문, 주문 상품을 생성한다.
	 */
	private TestFixture createTestFixture() {
		return transactionTemplate.execute(status -> {
			PromotionProduct promotionProduct = PromotionProduct.create(
				PRODUCT_ID,
				"결제 실패 동시성 테스트 특가 상품",
				UNIT_PRICE,
				30,
				TOTAL_EVENT_STOCK,
				LocalDateTime.now().minusMinutes(10),
				LocalDateTime.now().plusHours(1)
			);

			promotionProduct.open();

			// 선착순 구매로 주문 2건이 생성되면서 이벤트 재고 2개가 선점된 상황을 만든다.
			promotionProduct.decreaseStock(PURCHASE_QUANTITY * REQUEST_COUNT);

			PromotionProduct savedPromotionProduct = promotionProductRepository
				.saveAndFlush(promotionProduct);

			PromotionOrder order1 = promotionOrderRepository.saveAndFlush(
				PromotionOrder.create(
					MEMBER_ID_1,
					savedPromotionProduct.getId(),
					ORDER_AMOUNT,
					LocalDateTime.now()
				)
			);

			PromotionOrder order2 = promotionOrderRepository.saveAndFlush(
				PromotionOrder.create(
					MEMBER_ID_2,
					savedPromotionProduct.getId(),
					ORDER_AMOUNT,
					LocalDateTime.now()
				)
			);

			PromotionOrderItem orderItem1 = PromotionOrderItem.create(
				order1.getId(),
				savedPromotionProduct.getId(),
				PRODUCT_ID,
				"결제 실패 동시성 테스트 상품",
				PURCHASE_QUANTITY,
				UNIT_PRICE
			);

			PromotionOrderItem orderItem2 = PromotionOrderItem.create(
				order2.getId(),
				savedPromotionProduct.getId(),
				PRODUCT_ID,
				"결제 실패 동시성 테스트 상품",
				PURCHASE_QUANTITY,
				UNIT_PRICE
			);

			promotionOrderItemRepository.saveAndFlush(orderItem1);
			promotionOrderItemRepository.saveAndFlush(orderItem2);

			return new TestFixture(
				savedPromotionProduct.getId(),
				order1.getId(),
				order2.getId()
			);
		});
	}

	/**
	 * 결제 실패 처리 요청에 필요한 값이다.
	 */
	private record FailCommand(
		Long memberId,
		Long orderId,
		String portOnePaymentId
	) {
	}

	/**
	 * 테스트 데이터 생성 후 필요한 ID만 담아두는 값이다.
	 */
	private record TestFixture(
		Long promotionProductId,
		Long orderId1,
		Long orderId2
	) {
	}
}
