package com.example.team8salecommerce.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseRequest;
import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseResponse;
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
import com.example.team8salecommerce.global.exception.CustomException;

/**
 * 선착순 특가 구매 Service 테스트
 *
 * Redis Lock은 Facade 책임이므로 여기서는 테스트하지 않는다.
 * Service에서는 Lock 획득 이후 실제 DB 변경 흐름을 검증한다.
 *
 * 검증 대상:
 * - 이벤트 재고 차감
 * - 특가 주문 생성
 * - 특가 주문 상품 생성
 * - 재고 변경 이력 저장
 * - 실패 시 주문/이력 미생성
 */
@ExtendWith(MockitoExtension.class)
class PromotionPurchaseServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private PromotionProductRepository promotionProductRepository;

	@Mock
	private PromotionOrderRepository promotionOrderRepository;

	@Mock
	private PromotionOrderItemRepository promotionOrderItemRepository;

	@Mock
	private StockHistoryRepository stockHistoryRepository;

	@InjectMocks
	private PromotionPurchaseService promotionPurchaseService;

	@Test
	@DisplayName("선착순 구매에 성공하면 재고 차감, 주문 생성, 주문 상품 생성, 재고 이력 저장이 함께 처리된다")
	void purchaseWithTransaction_success() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		Long promotionProductId = 10L;
		Long promotionOrderId = 1000L;

		PromotionPurchaseRequest request = new PromotionPurchaseRequest(2);

		PromotionProduct promotionProduct = createOpenPromotionProduct(
			promotionProductId,
			productId,
			10
		);

		Product product = createMockProduct(productId, "테스트 상품");

		when(promotionProductRepository.findById(promotionProductId))
			.thenReturn(Optional.of(promotionProduct));

		when(productRepository.findByIdWithCategory(productId))
			.thenReturn(Optional.of(product));

		when(promotionOrderRepository.save(any(PromotionOrder.class)))
			.thenAnswer(invocation -> {
				PromotionOrder promotionOrder = invocation.getArgument(0);

				// 실제 DB 저장이 아니므로 테스트에서 주문 ID를 직접 넣어준다.
				ReflectionTestUtils.setField(promotionOrder, "id", promotionOrderId);

				return promotionOrder;
			});

		when(promotionOrderItemRepository.save(any(PromotionOrderItem.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		when(stockHistoryRepository.save(any(StockHistory.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		PromotionPurchaseResponse response = promotionPurchaseService.purchaseWithTransaction(
			memberId,
			promotionProductId,
			request
		);

		// then
		assertThat(response.orderId()).isEqualTo(promotionOrderId);
		assertThat(response.promotionProductId()).isEqualTo(promotionProductId);
		assertThat(response.quantity()).isEqualTo(2);
		assertThat(response.totalPrice()).isEqualTo(14000L);
		assertThat(response.orderStatus()).isEqualTo(PromotionOrderStatus.WAITING.name());
		assertThat(response.remainingEventStock()).isEqualTo(8);

		// 이벤트 재고가 10개에서 8개로 차감됐는지 확인한다.
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(8);

		ArgumentCaptor<PromotionOrder> orderCaptor = ArgumentCaptor.forClass(PromotionOrder.class);
		verify(promotionOrderRepository).save(orderCaptor.capture());

		PromotionOrder savedOrder = orderCaptor.getValue();

		assertThat(savedOrder.getMemberId()).isEqualTo(memberId);
		assertThat(savedOrder.getPromotionProductId()).isEqualTo(promotionProductId);
		assertThat(savedOrder.getTotalAmount()).isEqualTo(14000L);
		assertThat(savedOrder.getStatus()).isEqualTo(PromotionOrderStatus.WAITING);

		ArgumentCaptor<PromotionOrderItem> orderItemCaptor = ArgumentCaptor.forClass(PromotionOrderItem.class);
		verify(promotionOrderItemRepository).save(orderItemCaptor.capture());

		PromotionOrderItem savedOrderItem = orderItemCaptor.getValue();

		assertThat(savedOrderItem.getPromotionOrderId()).isEqualTo(promotionOrderId);
		assertThat(savedOrderItem.getPromotionProductId()).isEqualTo(promotionProductId);
		assertThat(savedOrderItem.getProductId()).isEqualTo(productId);
		assertThat(savedOrderItem.getProductName()).isEqualTo("테스트 상품");
		assertThat(savedOrderItem.getQuantity()).isEqualTo(2);
		assertThat(savedOrderItem.getUnitPrice()).isEqualTo(7000L);
		assertThat(savedOrderItem.getTotalPrice()).isEqualTo(14000L);

		ArgumentCaptor<StockHistory> stockHistoryCaptor = ArgumentCaptor.forClass(StockHistory.class);
		verify(stockHistoryRepository).save(stockHistoryCaptor.capture());

		StockHistory savedStockHistory = stockHistoryCaptor.getValue();

		assertThat(savedStockHistory.getProductId()).isEqualTo(productId);
		assertThat(savedStockHistory.getPromotionProductId()).isEqualTo(promotionProductId);
		assertThat(savedStockHistory.getOrderId()).isEqualTo(promotionOrderId);
		assertThat(savedStockHistory.getPaymentId()).isNull();
		assertThat(savedStockHistory.getRefundId()).isNull();
		assertThat(savedStockHistory.getType()).isEqualTo(StockChangeType.DECREASE);
		assertThat(savedStockHistory.getReason()).isEqualTo(StockChangeReason.PROMOTION_PURCHASE);
		assertThat(savedStockHistory.getQuantity()).isEqualTo(2);
		assertThat(savedStockHistory.getStockBefore()).isEqualTo(10);
		assertThat(savedStockHistory.getStockAfter()).isEqualTo(8);
	}

	@Test
	@DisplayName("남은 이벤트 재고보다 많은 수량을 구매하면 주문과 재고 이력이 생성되지 않는다")
	void purchaseWithTransaction_failWhenOutOfStock() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		Long promotionProductId = 10L;

		PromotionPurchaseRequest request = new PromotionPurchaseRequest(11);

		PromotionProduct promotionProduct = createOpenPromotionProduct(
			promotionProductId,
			productId,
			10
		);

		// 재고 부족 테스트에서는 Product의 id/name까지 사용하지 않는다.
		// 따라서 불필요한 stubbing이 생기지 않도록 단순 Mock만 사용한다.
		Product product = mock(Product.class);

		when(promotionProductRepository.findById(promotionProductId))
			.thenReturn(Optional.of(promotionProduct));

		when(productRepository.findByIdWithCategory(productId))
			.thenReturn(Optional.of(product));

		// when & then
		assertThatThrownBy(() -> promotionPurchaseService.purchaseWithTransaction(
			memberId,
			promotionProductId,
			request
		)).isInstanceOf(CustomException.class);

		// 실패했으므로 재고는 그대로 유지되어야 한다.
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(10);

		// 실패했으므로 주문, 주문 상품, 재고 이력은 생성되지 않아야 한다.
		verify(promotionOrderRepository, never()).save(any(PromotionOrder.class));
		verify(promotionOrderItemRepository, never()).save(any(PromotionOrderItem.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("구매 수량이 0이면 Repository를 호출하지 않고 실패한다")
	void purchaseWithTransaction_failWhenQuantityIsZero() {
		// given
		Long memberId = 1L;
		Long promotionProductId = 10L;
		PromotionPurchaseRequest request = new PromotionPurchaseRequest(0);

		// when & then
		assertThatThrownBy(() -> promotionPurchaseService.purchaseWithTransaction(
			memberId,
			promotionProductId,
			request
		)).isInstanceOf(CustomException.class);

		// 요청 기본 검증에서 실패하므로 어떤 Repository도 호출되지 않아야 한다.
		verifyNoInteractions(promotionProductRepository);
		verifyNoInteractions(productRepository);
		verifyNoInteractions(promotionOrderRepository);
		verifyNoInteractions(promotionOrderItemRepository);
		verifyNoInteractions(stockHistoryRepository);
	}

	/**
	 * 구매 가능한 상태의 테스트용 특가 상품을 생성한다.
	 */
	private PromotionProduct createOpenPromotionProduct(
		Long promotionProductId,
		Long productId,
		Integer totalEventStock
	) {
		LocalDateTime now = LocalDateTime.now();

		PromotionProduct promotionProduct = PromotionProduct.create(
			productId,
			"테스트 특가 상품",
			7000L,
			30,
			totalEventStock,
			now.minusMinutes(10),
			now.plusMinutes(10)
		);

		// 실제 DB 저장이 아니므로 테스트에서 특가 상품 ID를 직접 넣어준다.
		ReflectionTestUtils.setField(promotionProduct, "id", promotionProductId);

		promotionProduct.open();

		return promotionProduct;
	}

	/**
	 * Product Entity는 현재 테스트에서 필요한 id/name만 사용하므로 Mock으로 만든다.
	 */
	private Product createMockProduct(Long productId, String productName) {
		Product product = mock(Product.class);

		when(product.getId()).thenReturn(productId);
		when(product.getName()).thenReturn(productName);

		return product;
	}
}
