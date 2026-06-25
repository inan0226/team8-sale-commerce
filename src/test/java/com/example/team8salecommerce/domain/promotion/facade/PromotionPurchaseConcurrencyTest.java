package com.example.team8salecommerce.domain.promotion.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseRequest;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.domain.stock.entity.StockChangeReason;
import com.example.team8salecommerce.domain.stock.entity.StockChangeType;
import com.example.team8salecommerce.domain.stock.entity.StockHistory;
import com.example.team8salecommerce.domain.stock.repository.StockHistoryRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import jakarta.persistence.EntityManager;

/**
 * 선착순 구매 Redis Lock 동시성 테스트
 *
 * 기존 PromotionPurchaseServiceTest는 Service 단위 테스트라서
 * Redis Lock 실제 동작을 검증하지 않는다.
 *
 * 이 테스트는 PromotionPurchaseFacade를 호출해서
 * Redis Lock -> Transactional Service -> 재고 차감 -> 주문 생성 -> 재고 이력 저장
 * 흐름이 동시에 들어온 요청에서도 안전하게 동작하는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PromotionPurchaseConcurrencyTest {

	private static final int EVENT_STOCK = 5;
	private static final int REQUEST_COUNT = 20;
	private static final int PURCHASE_QUANTITY = 1;

	@Autowired
	private PromotionPurchaseFacade promotionPurchaseFacade;

	@Autowired
	private PromotionProductRepository promotionProductRepository;

	@Autowired
	private PromotionOrderRepository promotionOrderRepository;

	@Autowired
	private PromotionOrderItemRepository promotionOrderItemRepository;

	@Autowired
	private StockHistoryRepository stockHistoryRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		// 다른 테스트 데이터가 결과에 영향을 주지 않도록
		// 동시성 테스트에서 사용하는 테이블을 먼저 비운다.
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from StockHistory").executeUpdate();
			entityManager.createQuery("delete from PromotionOrderItem").executeUpdate();
			entityManager.createQuery("delete from PromotionOrder").executeUpdate();
			entityManager.createQuery("delete from PromotionProduct").executeUpdate();
			entityManager.createQuery("delete from Product").executeUpdate();
			entityManager.createQuery("delete from Category").executeUpdate();
		});
	}

	@Test
	@DisplayName("동시에 여러 명이 구매해도 이벤트 재고를 초과해 판매되지 않는다")
	void purchaseConcurrentlySuccessWithoutOverselling() throws Exception {
		// given
		TestFixture fixture = createTestFixture();

		ExecutorService executorService = Executors.newFixedThreadPool(REQUEST_COUNT);

		// 모든 스레드가 준비될 때까지 기다리는 용도다.
		CountDownLatch readyLatch = new CountDownLatch(REQUEST_COUNT);

		// 모든 스레드가 거의 동시에 구매를 시작하게 만드는 용도다.
		CountDownLatch startLatch = new CountDownLatch(1);

		// 모든 스레드의 구매 시도가 끝났는지 확인하는 용도다.
		CountDownLatch doneLatch = new CountDownLatch(REQUEST_COUNT);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		// 실패 요청이 어떤 이유로 실패했는지 구분해서 검증한다.
		// Redis Lock 기반 선착순 테스트이므로 재고 부족과 락 획득 실패를 분리해서 확인한다.
		AtomicInteger outOfStockFailCount = new AtomicInteger();
		AtomicInteger lockAcquireFailCount = new AtomicInteger();

		// 재고가 모두 소진되어 특가 상품 상태가 SOLD_OUT으로 변경된 뒤,
		// 이후 요청이 들어오면 PROMOTION_NOT_OPEN으로 실패할 수 있다.
		AtomicInteger promotionNotOpenFailCount = new AtomicInteger();

		// 예상하지 못한 예외가 발생하면 테스트 마지막에 확인하기 위해 모아둔다.
		List<Throwable> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());

		PromotionPurchaseRequest request = new PromotionPurchaseRequest(PURCHASE_QUANTITY);

		// when
		for (int i = 0; i < REQUEST_COUNT; i++) {
			Long memberId = (long) i + 1;

			executorService.submit(() -> {
				try {
					readyLatch.countDown();
					startLatch.await();

					promotionPurchaseFacade.purchase(
						memberId,
						fixture.promotionProductId(),
						request
					);

					successCount.incrementAndGet();
				} catch (CustomException e) {
					// 동시 구매 실패 원인을 구분해서 집계한다.
					// 성공하지 못한 요청은 재고 부족, 락 획득 실패, 이벤트 미오픈 상태 중 하나여야 한다.
					if (e.getErrorCode() == ErrorCode.OUT_OF_STOCK) {
						outOfStockFailCount.incrementAndGet();
						failCount.incrementAndGet();
					} else if (e.getErrorCode() == ErrorCode.LOCK_ACQUIRE_FAILED) {
						lockAcquireFailCount.incrementAndGet();
						failCount.incrementAndGet();
					} else if (e.getErrorCode() == ErrorCode.PROMOTION_NOT_OPEN) {
						promotionNotOpenFailCount.incrementAndGet();
						failCount.incrementAndGet();
					} else {
						// 예상하지 못한 CustomException은 테스트 실패로 처리한다.
						unexpectedExceptions.add(e);
					}
				} catch (Throwable e) {
					// 의도하지 않은 예외는 테스트 실패로 처리한다.
					unexpectedExceptions.add(e);
				} finally {
					doneLatch.countDown();
				}
			});
		}

		boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
		assertThat(ready).isTrue();

		// 준비된 스레드들을 동시에 출발시킨다.
		startLatch.countDown();

		boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
		assertThat(completed).isTrue();

		executorService.shutdown();

		// then
		assertThat(unexpectedExceptions).isEmpty();

		// 성공 요청 수는 이벤트 재고를 초과하면 안 된다.
		// Redis Lock 획득 실패가 발생할 수 있으므로 항상 EVENT_STOCK과 정확히 같다고 고정하지 않는다.
		assertThat(successCount.get()).isLessThanOrEqualTo(EVENT_STOCK);

		// 전체 요청 수는 성공 요청 + 실패 요청으로 나뉘어야 한다.
		assertThat(successCount.get() + failCount.get()).isEqualTo(REQUEST_COUNT);

		// 실패한 요청은 모두 재고 부족, Redis Lock 획득 실패,
		// 또는 재고 소진 후 이벤트 미오픈 상태 중 하나여야 한다.
		assertThat(
			outOfStockFailCount.get()
				+ lockAcquireFailCount.get()
				+ promotionNotOpenFailCount.get()
		).isEqualTo(failCount.get());

		PromotionProduct promotionProduct = promotionProductRepository.findById(fixture.promotionProductId())
			.orElseThrow();

		// 남은 이벤트 재고는 음수로 내려가면 안 된다.
		assertThat(promotionProduct.getRemainingEventStock()).isGreaterThanOrEqualTo(0);

		// 성공한 구매 수와 남은 재고의 합은 최초 이벤트 재고와 같아야 한다.
		assertThat(successCount.get() + promotionProduct.getRemainingEventStock())
			.isEqualTo(EVENT_STOCK);

		List<PromotionOrder> promotionOrders = promotionOrderRepository.findAll();
		List<PromotionOrderItem> promotionOrderItems = promotionOrderItemRepository.findAll();
		List<StockHistory> stockHistories = stockHistoryRepository.findAll();

		// 성공한 요청만 주문/주문상품/재고이력을 생성해야 한다.
		assertThat(promotionOrders).hasSize(successCount.get());
		assertThat(promotionOrderItems).hasSize(successCount.get());
		assertThat(stockHistories).hasSize(successCount.get());

		// 실패한 요청에서는 재고 이력이 생성되지 않아야 하므로
		// 재고 이력 개수는 실제 성공 요청 수와 같아야 한다.
		assertThat(stockHistories).allSatisfy(stockHistory -> {
			assertThat(stockHistory.getProductId()).isEqualTo(fixture.productId());
			assertThat(stockHistory.getPromotionProductId()).isEqualTo(fixture.promotionProductId());
			assertThat(stockHistory.getType()).isEqualTo(StockChangeType.DECREASE);
			assertThat(stockHistory.getReason()).isEqualTo(StockChangeReason.PROMOTION_PURCHASE);
			assertThat(stockHistory.getQuantity()).isEqualTo(PURCHASE_QUANTITY);
			assertThat(stockHistory.getStockAfter()).isGreaterThanOrEqualTo(0);
		});
	}

	/**
	 * 동시성 테스트에 필요한 상품, 카테고리, 특가 상품을 생성한다.
	 *
	 * Product와 Category에는 현재 테스트용 팩토리 메서드가 없기 때문에
	 * ReflectionTestUtils로 필요한 필드만 채운다.
	 */
	private TestFixture createTestFixture() {
		return transactionTemplate.execute(status -> {
			Category category = createCategory();
			entityManager.persist(category);

			Product product = createProduct(category);
			entityManager.persist(product);

			PromotionProduct promotionProduct = PromotionProduct.create(
				product.getId(),
				"동시성 테스트 특가 상품",
				7000L,
				30,
				EVENT_STOCK,
				LocalDateTime.now().minusMinutes(10),
				LocalDateTime.now().plusMinutes(10)
			);

			// 선착순 구매가 가능하도록 특가 상품 상태를 OPEN으로 변경한다.
			promotionProduct.open();

			promotionProductRepository.save(promotionProduct);
			entityManager.flush();

			return new TestFixture(
				product.getId(),
				promotionProduct.getId()
			);
		});
	}

	/**
	 * 테스트용 카테고리를 생성한다.
	 */
	private Category createCategory() {
		// Category의 기본 생성자는 protected라서 테스트 클래스에서 직접 new Category()를 할 수 없다.
		// 테스트 데이터 생성을 위해 리플렉션으로 기본 생성자를 호출한다.
		Category category = createEntity(Category.class);

		ReflectionTestUtils.setField(category, "name", "테스트 카테고리");

		return category;
	}

	/**
	 * 테스트용 상품을 생성한다.
	 *
	 * ProductRepository.findByIdWithCategory()는
	 * isDeleted = false 조건과 category fetch join을 사용하므로
	 * category와 isDeleted 값을 반드시 세팅한다.
	 */
	private Product createProduct(Category category) {
		// Product의 기본 생성자도 protected라서 테스트 클래스에서 직접 new Product()를 할 수 없다.
		// 테스트 데이터 생성을 위해 리플렉션으로 기본 생성자를 호출한다.
		Product product = createEntity(Product.class);

		ReflectionTestUtils.setField(product, "name", "동시성 테스트 상품");
		ReflectionTestUtils.setField(product, "brand", "테스트 브랜드");
		ReflectionTestUtils.setField(product, "price", 10000L);
		ReflectionTestUtils.setField(product, "stock", 100);
		ReflectionTestUtils.setField(product, "imageUrl", "test-image.jpg");
		ReflectionTestUtils.setField(product, "description", "동시성 테스트용 상품");
		ReflectionTestUtils.setField(product, "isDeleted", false);
		ReflectionTestUtils.setField(product, "viewCount", 0);
		ReflectionTestUtils.setField(product, "category", category);

		return product;
	}

	/**
	 * protected 기본 생성자를 가진 엔티티를 테스트에서 생성하기 위한 헬퍼 메서드다.
	 *
	 * 운영 코드에서는 엔티티 생성 규칙을 지켜야 하지만,
	 * 이 테스트에서는 Product/Category 테스트 데이터를 준비해야 하므로
	 * 리플렉션으로 기본 생성자를 호출한다.
	 */
	private <T> T createEntity(Class<T> entityClass) {
		try {
			Constructor<T> constructor = entityClass.getDeclaredConstructor();
			constructor.setAccessible(true);

			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", e);
		}
	}

	/**
	 * 테스트 데이터 생성 후 필요한 ID만 담아두는 record다.
	 */
	private record TestFixture(
		Long productId,
		Long promotionProductId
	) {
	}
}
