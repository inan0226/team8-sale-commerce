package com.example.team8salecommerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.entity.OrderItem;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;


class OrderEntityTest {

    @Test
    @DisplayName("새 주문은 결제 대기 상태로 생성되고 한 번만 취소할 수 있다")
    void orderStatusTransition() {
        // given: 유효한 회원과 주문 금액으로 주문을 생성
        Order order = Order.create(createMember(), 20_000L, LocalDateTime.now());

        // when: 결제 대기 주문을 취소.
        order.cancel();

        // then: 취소 상태가 되며 재취소는 허용되지 않음
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(order::cancel)
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
    }

    @Test
    @DisplayName("일반 주문 상품은 일반 판매 가격을 스냅샷으로 저장한다")
    void normalOrderItemSnapshot() {
        // given: 일반 상품과 공통 주문을 준비
        Product product = createProduct();
        Order order = Order.create(createMember(), 20_000L, LocalDateTime.now());

        // when: 일반 주문 상품을 생성
        OrderItem orderItem = OrderItem.create(order, product, 2);

        // then: 일반 가격과 수량을 기준으로 금액이 계산
        assertThat(orderItem.getProductName()).isEqualTo("키보드");
        assertThat(orderItem.getProductPrice()).isEqualTo(10_000L);
        assertThat(orderItem.calculateTotalPrice()).isEqualTo(20_000L);
    }

    private Member createMember() {
        return Member.create("member@example.com", "encoded-password", "회원");
    }


    private Product createProduct() {
        Product product = Product.create(
                "키보드",
                "브랜드",
                10_000L,
                5,
                "image.png",
                "상품 설명",
                Mockito.mock(Category.class)
        );
        ReflectionTestUtils.setField(product, "id", 10L);
        return product;
    }
}
