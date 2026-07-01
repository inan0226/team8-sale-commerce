package com.example.team8salecommerce.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.team8salecommerce.domain.cart.entity.Cart;
import com.example.team8salecommerce.domain.cart.entity.CartItem;
import com.example.team8salecommerce.domain.cart.repository.CartItemRepository;
import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class CartItemRepositoryTest {

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("같은 장바구니의 활성 항목만 일괄 soft delete한다")
	void softDeleteActiveByIdsDeletesOnlyMatchingActiveItems() {
		Category category = persistCategory();
		Cart targetCart = persistCart("member1@test.com", "member1");
		Cart otherCart = persistCart("member2@test.com", "member2");
		CartItem first = persistCartItem(targetCart, persistProduct(category, "상품1"), false);
		CartItem second = persistCartItem(targetCart, persistProduct(category, "상품2"), false);
		CartItem alreadyDeleted = persistCartItem(targetCart, persistProduct(category, "상품3"), true);
		CartItem otherCartItem = persistCartItem(otherCart, persistProduct(category, "상품4"), false);
		LocalDateTime deletedAt = LocalDateTime.of(2026, 7, 1, 12, 0);
		flushAndClear();

		int affectedRows = cartItemRepository.softDeleteActiveByIds(
			targetCart.getId(),
			List.of(first.getId(), second.getId(), alreadyDeleted.getId(), otherCartItem.getId()),
			deletedAt
		);
		entityManager.clear();

		assertThat(affectedRows).isEqualTo(2);
		assertThat(findCartItem(first.getId()).getDeletedAt()).isEqualTo(deletedAt);
		assertThat(findCartItem(second.getId()).getDeletedAt()).isEqualTo(deletedAt);
		assertThat(findCartItem(alreadyDeleted.getId()).getDeletedAt()).isNotNull();
		assertThat(findCartItem(otherCartItem.getId()).getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("이미 삭제된 장바구니 항목은 다시 soft delete하지 않는다")
	void softDeleteActiveByIdsReturnsZeroForDeletedItems() {
		Category category = persistCategory();
		Cart cart = persistCart("member@test.com", "member");
		CartItem cartItem = persistCartItem(cart, persistProduct(category, "상품"), true);
		flushAndClear();
		LocalDateTime originalDeletedAt = findCartItem(cartItem.getId()).getDeletedAt();
		entityManager.clear();

		int affectedRows = cartItemRepository.softDeleteActiveByIds(
			cart.getId(),
			List.of(cartItem.getId()),
			LocalDateTime.of(2026, 7, 1, 13, 0)
		);
		entityManager.clear();

		assertThat(affectedRows).isZero();
		assertThat(findCartItem(cartItem.getId()).getDeletedAt()).isEqualTo(originalDeletedAt);
	}

	private Category persistCategory() {
		Category category = Category.create("repository-test-category");
		entityManager.persist(category);
		return category;
	}

	private Cart persistCart(String email, String nickname) {
		Member member = Member.create(email, "password", nickname);
		entityManager.persist(member);
		Cart cart = Cart.create(member);
		entityManager.persist(cart);
		return cart;
	}

	private Product persistProduct(Category category, String name) {
		Product product = Product.create(name, "브랜드", 1_000L, 10, "image", "설명", category);
		entityManager.persist(product);
		return product;
	}

	private CartItem persistCartItem(Cart cart, Product product, boolean deleted) {
		CartItem cartItem = CartItem.create(cart, product, 1);
		if (deleted) {
			cartItem.delete();
		}
		entityManager.persist(cartItem);
		return cartItem;
	}

	private CartItem findCartItem(Long cartItemId) {
		return entityManager.find(CartItem.class, cartItemId);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
