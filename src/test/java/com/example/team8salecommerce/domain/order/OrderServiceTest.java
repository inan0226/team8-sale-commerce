package com.example.team8salecommerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.cart.entity.Cart;
import com.example.team8salecommerce.domain.cart.entity.CartItem;
import com.example.team8salecommerce.domain.cart.repository.CartItemRepository;
import com.example.team8salecommerce.domain.cart.repository.CartRepository;
import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.team8salecommerce.domain.order.dto.response.OrderListResponse;
import com.example.team8salecommerce.domain.order.dto.response.OrderResponse;
import com.example.team8salecommerce.domain.order.entity.Order;
import com.example.team8salecommerce.domain.order.entity.OrderItem;
import com.example.team8salecommerce.domain.order.enumtype.OrderStatus;
import com.example.team8salecommerce.domain.order.repository.OrderItemRepository;
import com.example.team8salecommerce.domain.order.repository.OrderProductRepository;
import com.example.team8salecommerce.domain.order.repository.OrderRepository;
import com.example.team8salecommerce.domain.order.service.OrderService;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {


    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;


    @Mock
    private OrderItemRepository orderItemRepository;


    @Mock
    private CartRepository cartRepository;


    @Mock
    private CartItemRepository cartItemRepository;


    @Mock
    private OrderProductRepository orderProductRepository;


    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("장바구니 상품으로 주문을 생성하면 재고가 차감되고 장바구니 항목이 비활성화된다")
    void createOrderSuccess() {
        // given: 재고 5개의 상품 2개를 담은 회원 장바구니를 준비
        Member member = createMember(1L);
        Cart cart = createCart(member, 2L);
        Product product = createProduct(10L, 10_000L, 5);
        CartItem cartItem = createCartItem(cart, product, 100L, 2);
        CreateOrderRequest request = new CreateOrderRequest(List.of(100L));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findActiveCartItemsByIds(2L, List.of(100L)))
                .thenReturn(List.of(cartItem));
        when(orderProductRepository.findAllActiveByIdInForUpdate(List.of(10L)))
                .thenReturn(List.of(product));
        when(orderProductRepository.decreaseStock(10L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1000L);
            return order;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when: 주문을 생성
        OrderResponse response = orderService.createOrder(1L, request);

        // then: 주문 금액/상태, 재고 차감, 장바구니 비활성화를 확인
        assertThat(response.orderId()).isEqualTo(1000L);
        assertThat(response.totalPrice()).isEqualTo(20_000L);
        assertThat(response.status()).isEqualTo(OrderStatus.WAITING);
        assertThat(response.items()).hasSize(1);
        assertThat(cartItem.isDeleted()).isTrue();
        verify(orderProductRepository).decreaseStock(10L, 2);
        verify(orderItemRepository).saveAll(any());
    }

    @Test
    @DisplayName("상품 재고가 부족하면 주문을 저장하지 않는다")
    void createOrderFailsWhenOutOfStock() {
        // given: 재고 1개인 상품을 2개 주문하도록 준비
        Member member = createMember(1L);
        Cart cart = createCart(member, 2L);
        Product product = createProduct(10L, 10_000L, 1);
        CartItem cartItem = createCartItem(cart, product, 100L, 2);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findActiveCartItemsByIds(2L, List.of(100L)))
                .thenReturn(List.of(cartItem));
        when(orderProductRepository.findAllActiveByIdInForUpdate(List.of(10L)))
                .thenReturn(List.of(product));
        when(orderProductRepository.decreaseStock(10L, 2)).thenReturn(0);

        // when & then: 재고 부족 예외와 주문 미저장을 확인
        assertThatThrownBy(() -> orderService.createOrder(
                1L,
                new CreateOrderRequest(List.of(100L))
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.OUT_OF_STOCK.getMessage());

        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("회원의 주문 목록을 주문 상품과 함께 조회한다")
    void getOrdersSuccess() {
        // given: 회원의 주문 한 건과 주문 상품 한 건을 준비
        Member member = createMember(1L);
        Product product = createProduct(10L, 10_000L, 5);
        Order order = createOrder(member, 1000L, 20_000L);
        OrderItem orderItem = OrderItem.create(order, product, 2);
        ReflectionTestUtils.setField(orderItem, "id", 2000L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(orderRepository.findAllByMemberIdOrderByOrderedAtDesc(1L))
                .thenReturn(List.of(order));
        when(orderItemRepository.findAllByOrderIdIn(List.of(1000L)))
                .thenReturn(List.of(orderItem));

        // when: 주문 목록을 조회
        OrderListResponse response = orderService.getOrders(1L);

        // then: 주문과 상품 스냅샷이 함께 반환
        assertThat(response.orders()).hasSize(1);
        assertThat(response.orders().getFirst().items()).hasSize(1);
        assertThat(response.orders().getFirst().items().getFirst().productName())
                .isEqualTo("키보드");
    }

    @Test
    @DisplayName("결제 대기 주문을 취소하면 상품 재고가 복구된다")
    void cancelOrderSuccess() {
        // given: 재고가 이미 2개 차감된 결제 대기 주문을 준비
        Member member = createMember(1L);
        Product product = createProduct(10L, 10_000L, 3);
        Order order = createOrder(member, 1000L, 20_000L);
        OrderItem orderItem = OrderItem.create(order, product, 2);

        when(orderRepository.findByIdAndMemberIdForUpdate(1000L, 1L))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(1000L)).thenReturn(List.of(orderItem));
        when(orderProductRepository.findAllByIdInForUpdate(List.of(10L)))
                .thenReturn(List.of(product));
        when(orderProductRepository.restoreStock(10L, 2)).thenReturn(1);

        // when: 주문을 취소.
        OrderResponse response = orderService.cancelOrder(1L, 1000L);

        // then: 상태가 취소로 바뀌고 재고가 원래 수량으로 복구
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderProductRepository).restoreStock(10L, 2);
    }

    @Test
    @DisplayName("이미 취소된 주문은 다시 취소할 수 없다")
    void cancelOrderFailsWhenStatusIsNotWaiting() {
        // given: 이미 취소된 주문을 준비
        Member member = createMember(1L);
        Order order = createOrder(member, 1000L, 20_000L);
        order.cancel();
        when(orderRepository.findByIdAndMemberIdForUpdate(1000L, 1L))
                .thenReturn(Optional.of(order));

        // when & then: 상태 예외가 발생하고 주문 상품을 조회하지 않음
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1000L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());

        verify(orderItemRepository, never()).findAllByOrderId(1000L);
    }


    private Member createMember(Long memberId) {
        Member member = Member.create("member@example.com", "encoded-password", "회원");
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }


    private Cart createCart(Member member, Long cartId) {
        Cart cart = Cart.create(member);
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }


    private Product createProduct(Long productId, Long price, Integer stock) {
        Product product = Product.create(
                "키보드",
                "브랜드",
                price,
                stock,
                "image.png",
                "상품 설명",
                org.mockito.Mockito.mock(Category.class)
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }


    private CartItem createCartItem(
            Cart cart,
            Product product,
            Long cartItemId,
            Integer quantity
    ) {
        CartItem cartItem = CartItem.create(cart, product, quantity);
        ReflectionTestUtils.setField(cartItem, "id", cartItemId);
        return cartItem;
    }


    private Order createOrder(Member member, Long orderId, Long totalPrice) {
        Order order = Order.create(member, totalPrice, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }
}
