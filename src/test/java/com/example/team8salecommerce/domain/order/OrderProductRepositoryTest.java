package com.example.team8salecommerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.order.repository.OrderProductRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class OrderProductRepositoryTest {

	@Autowired
	private OrderProductRepository orderProductRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("재고가 충분한 활성 상품의 재고를 차감한다")
	void decreaseStockSuccess() {
		Product product = persistProduct(10, false);
		flushAndClear();

		int affectedRows = orderProductRepository.decreaseStock(product.getId(), 3);
		entityManager.clear();

		assertThat(affectedRows).isEqualTo(1);
		assertThat(findProduct(product.getId()).getStock()).isEqualTo(7);
	}

	@Test
	@DisplayName("재고가 부족하면 재고를 차감하지 않는다")
	void decreaseStockFailsWhenStockIsInsufficient() {
		Product product = persistProduct(2, false);
		flushAndClear();

		int affectedRows = orderProductRepository.decreaseStock(product.getId(), 3);
		entityManager.clear();

		assertThat(affectedRows).isZero();
		assertThat(findProduct(product.getId()).getStock()).isEqualTo(2);
	}

	@Test
	@DisplayName("삭제된 상품은 재고를 차감하지 않는다")
	void decreaseStockFailsWhenProductIsDeleted() {
		Product product = persistProduct(10, true);
		flushAndClear();

		int affectedRows = orderProductRepository.decreaseStock(product.getId(), 3);
		entityManager.clear();

		assertThat(affectedRows).isZero();
		assertThat(findProduct(product.getId()).getStock()).isEqualTo(10);
	}

	@Test
	@DisplayName("주문 취소 수량만큼 상품 재고를 복구한다")
	void restoreStockSuccess() {
		Product product = persistProduct(7, false);
		flushAndClear();

		int affectedRows = orderProductRepository.restoreStock(product.getId(), 3);
		entityManager.clear();

		assertThat(affectedRows).isEqualTo(1);
		assertThat(findProduct(product.getId()).getStock()).isEqualTo(10);
	}

	@Test
	@DisplayName("존재하지 않는 상품의 재고는 복구하지 않는다")
	void restoreStockFailsWhenProductDoesNotExist() {
		int affectedRows = orderProductRepository.restoreStock(Long.MAX_VALUE, 3);

		assertThat(affectedRows).isZero();
	}

	private Product persistProduct(int stock, boolean deleted) {
		Category category = Category.create("repository-test-category");
		entityManager.persist(category);

		Product product = deleted
			? Product.createDeleted("상품", "브랜드", 1_000L, stock, "image", "설명", category)
			: Product.create("상품", "브랜드", 1_000L, stock, "image", "설명", category);
		entityManager.persist(product);
		return product;
	}

	private Product findProduct(Long productId) {
		return entityManager.find(Product.class, productId);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
