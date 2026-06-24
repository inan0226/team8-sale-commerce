package com.example.team8salecommerce.domain.cart;

import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.response.CartItemResponse;
import com.example.team8salecommerce.domain.cart.dto.response.CartResponse;
import com.example.team8salecommerce.domain.cart.entity.Cart;
import com.example.team8salecommerce.domain.cart.entity.CartItem;
import com.example.team8salecommerce.domain.cart.repository.CartItemRepository;
import com.example.team8salecommerce.domain.cart.repository.CartRepository;
import com.example.team8salecommerce.domain.cart.service.CartService;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberRepository memberRepository;


    @Test
    @DisplayName("장바구니에 상품을 담았습니다.")
    void addCartItem_success() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);

        Product product = mock(Product.class);

        AddCartItemRequest request =
                new AddCartItemRequest(
                        10L,
                        2
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(product));

        when(product.getId()).thenReturn(10L);
        when(product.getName()).thenReturn("키보드");

        when(cartItemRepository.findByCartIdAndProductId(
                any(),
                any()
        )).thenReturn(Optional.empty());

        // when
        CartItemResponse response =
                cartService.addCartItem(
                        memberId,
                        request
                );

        // then
        verify(cartItemRepository).save(any());

        assertThat(response.productId())
                .isEqualTo(10L);
    }

    @Test
    @DisplayName("같은 상품을 다시 담으면 수량이 증가합니다")
    void addCartItem_increaseQuantity() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);

        Product product = mock(Product.class);

        CartItem cartItem =
                CartItem.create(
                        cart,
                        product,
                        2
                );

        AddCartItemRequest request =
                new AddCartItemRequest(
                        10L,
                        3
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(product));

        when(product.getId()).thenReturn(10L);

        when(cartItemRepository.findByCartIdAndProductId(
                any(),
                any()
        )).thenReturn(Optional.of(cartItem));

        // when
        cartService.addCartItem(
                memberId,
                request
        );

        // then
        assertThat(cartItem.getQuantity())
                .isEqualTo(5);

        verify(cartItemRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("삭제된 상품은 장바구니에 담을 수 없습니다.")
    void addCartItem_deletedProduct_fail() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);

        AddCartItemRequest request =
                new AddCartItemRequest(
                        10L,
                        1
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                cartService.addCartItem(
                        memberId,
                        request
                ))
                .isInstanceOf(CustomException.class)
                .hasMessage(
                        ErrorCode.PRODUCT_NOT_FOUND.getMessage()
                );
    }

    @Test
    @DisplayName("장바구니가 없으면 생성 후 상품을 추가합니다.")
    void addCartItem_createCart() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Product product = mock(Product.class);

        AddCartItemRequest request =
                new AddCartItemRequest(
                        10L,
                        1
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.empty());

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.save(any()))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(product));

        when(product.getId()).thenReturn(10L);

        when(cartItemRepository.findByCartIdAndProductId(
                any(),
                any()
        )).thenReturn(Optional.empty());

        // when
        cartService.addCartItem(
                memberId,
                request
        );

        // then
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("장바구니 조회에 성공하였습니다.")
    void getCart_success() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);

        ReflectionTestUtils.setField(
                cart,
                "id",
                1L
        );

        Product product = mock(Product.class);

        when(product.getId()).thenReturn(10L);
        when(product.getName()).thenReturn("키보드");
        when(product.getPrice()).thenReturn(10000L);

        CartItem cartItem =
                CartItem.create(
                        cart,
                        product,
                        2
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(1L))
                .thenReturn(List.of(cartItem));

        // when
        CartResponse response =
                cartService.getCart(memberId);

        // then
        assertThat(response.items())
                .hasSize(1);

        assertThat(response.totalPrice())
                .isEqualTo(20000L);
    }
}
