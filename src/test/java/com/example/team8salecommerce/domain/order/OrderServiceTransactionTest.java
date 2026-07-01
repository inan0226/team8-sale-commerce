package com.example.team8salecommerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.team8salecommerce.domain.cart.entity.Cart;
import com.example.team8salecommerce.domain.cart.entity.CartItem;
import com.example.team8salecommerce.domain.cart.repository.CartItemRepository;
import com.example.team8salecommerce.domain.cart.repository.CartRepository;
import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.category.repository.CategoryRepository;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.team8salecommerce.domain.order.repository.OrderItemRepository;
import com.example.team8salecommerce.domain.order.repository.OrderProductRepository;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.order.service.OrderService;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.config.JpaAuditingConfig;
import com.example.team8salecommerce.global.config.QueryDslConfig;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({OrderService.class, JpaAuditingConfig.class, QueryDslConfig.class})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderServiceTransactionTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private OrderProductRepository orderProductRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void cleanUpCommittedFixtures() {
		transactionTemplate.executeWithoutResult(status -> {
			orderItemRepository.deleteAllInBatch();
			orderRepository.deleteAllInBatch();
			cartItemRepository.deleteAllInBatch();
			cartRepository.deleteAllInBatch();
			orderProductRepository.deleteAllInBatch();
			categoryRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		});
	}

	@Test
	@DisplayName("주문 중 일부 상품의 재고 차감이 실패하면 앞선 재고 차감도 rollback한다")
	void createOrderRollsBackPreviouslyDecreasedStock() {
		Fixture fixture = transactionTemplate.execute(status -> persistFixture());

		assertThatThrownBy(() -> orderService.createOrder(
			fixture.memberId(),
			new CreateOrderRequest(List.of(fixture.firstCartItemId(), fixture.secondCartItemId()))
		))
			.isInstanceOf(CustomException.class)
			.satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
				.isEqualTo(ErrorCode.OUT_OF_STOCK));

		assertThat(orderProductRepository.findById(fixture.firstProductId()))
			.get()
			.extracting(Product::getStock)
			.isEqualTo(10);
		assertThat(orderProductRepository.findById(fixture.secondProductId()))
			.get()
			.extracting(Product::getStock)
			.isEqualTo(1);
		assertThat(cartItemRepository.findById(fixture.firstCartItemId()))
			.get()
			.extracting(CartItem::getDeletedAt)
			.isNull();
		assertThat(cartItemRepository.findById(fixture.secondCartItemId()))
			.get()
			.extracting(CartItem::getDeletedAt)
			.isNull();
		assertThat(orderRepository.count()).isZero();
		assertThat(orderItemRepository.count()).isZero();
	}

	private Fixture persistFixture() {
		Member member = memberRepository.save(Member.create(
			"rollback@test.com",
			"password",
			"rollback-member"
		));
		Cart cart = cartRepository.save(Cart.create(member));
		Category category = categoryRepository.save(Category.create("rollback-test-category"));
		Product firstProduct = orderProductRepository.save(Product.create(
			"재고 충분 상품",
			"브랜드",
			1_000L,
			10,
			"image",
			"설명",
			category
		));
		Product secondProduct = orderProductRepository.save(Product.create(
			"재고 부족 상품",
			"브랜드",
			1_000L,
			1,
			"image",
			"설명",
			category
		));
		CartItem firstCartItem = cartItemRepository.save(CartItem.create(cart, firstProduct, 2));
		CartItem secondCartItem = cartItemRepository.save(CartItem.create(cart, secondProduct, 2));

		return new Fixture(
			member.getId(),
			firstProduct.getId(),
			secondProduct.getId(),
			firstCartItem.getId(),
			secondCartItem.getId()
		);
	}

	private record Fixture(
		Long memberId,
		Long firstProductId,
		Long secondProductId,
		Long firstCartItemId,
		Long secondCartItemId
	) {
	}
}
