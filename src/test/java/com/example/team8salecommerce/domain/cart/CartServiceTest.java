package com.example.team8salecommerce.domain.cart;

import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.request.UpdateCartItemRequest;
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

        ReflectionTestUtils.setField(
                cart,
                "id",
                1L
        );

        Product product = mock(Product.class);

        AddCartItemRequest request =
                new AddCartItemRequest(
                        10L,
                        2
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(product));

        when(product.getId()).thenReturn(10L);
        when(product.getName()).thenReturn("키보드");

        when(cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(
                any(),
                any()
        )).thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


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

        ReflectionTestUtils.setField(
                cart,
                "id",
                1L
        );

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

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(product));

        when(product.getId()).thenReturn(10L);


        when(cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(
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


        ReflectionTestUtils.setField(
                cart,
                "id",
                1L
        );

        AddCartItemRequest request =
                new AddCartItemRequest(
                        10L,
                        1
                );

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

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


        when(productRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(product));

        when(product.getId()).thenReturn(10L);

        when(cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(
                any(),
                any()
        )).thenReturn(Optional.empty());


        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


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

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartItemRepository.findActiveCartItems(1L))
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

    @Test
    @DisplayName("장바구니 상품의 수량을 변경하였습니다.")
    void updateCartItemQuantity_success() {

        // given
        Long memberId = 1L;
        Long cartItemId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);
        ReflectionTestUtils.setField(cart, "id", 1L);

        Product product = mock(Product.class);

        CartItem cartItem = CartItem.create(
                cart,
                product,
                2
        );

        ReflectionTestUtils.setField(cartItem, "id", cartItemId);

        UpdateCartItemRequest request =
                new UpdateCartItemRequest(5);

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByIdAndDeletedAtIsNull(cartItemId))
                .thenReturn(Optional.of(cartItem));

        when(member.getId())
                .thenReturn(memberId);

        when(product.getId()).thenReturn(10L);
        when(product.getName()).thenReturn("키보드");

        // when
        CartItemResponse response =
                cartService.updateCartItemQuantity(
                        memberId,
                        cartItemId,
                        request
                );
        // then
        assertThat(cartItem.getQuantity()).isEqualTo(5);
        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 상품은 수량을 변경할 수 없습니다.")
    void updateCartItemQuantity_cartItemNotFound() {

        // given
        Long memberId = 1L;
        Long cartItemId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByIdAndDeletedAtIsNull(cartItemId))
                .thenReturn(Optional.empty());

        when(member.getId())
                .thenReturn(memberId);

        // when & then
        assertThatThrownBy(() ->
                cartService.updateCartItemQuantity(
                        memberId,
                        cartItemId,
                        new UpdateCartItemRequest(3)
                ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("다른 회원의 장바구니 상품은 수정할 수 없습니다.")
    void updateCartItemQuantity_forbidden() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart myCart = Cart.create(member);
        ReflectionTestUtils.setField(myCart, "id", 1L);

        Cart anotherCart = Cart.create(member);
        ReflectionTestUtils.setField(anotherCart, "id", 2L);

        Product product = mock(Product.class);

        CartItem cartItem =
                CartItem.create(
                        anotherCart,
                        product,
                        2
                );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(myCart));

        when(cartItemRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(cartItem));

        when(member.getId())
                .thenReturn(memberId);

        // when & then
        assertThatThrownBy(() ->
                cartService.updateCartItemQuantity(
                        memberId,
                        1L,
                        new UpdateCartItemRequest(3)
                ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("장바구니 상품을 삭제하였습니다.")
    void deleteCartItem_success() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);
        ReflectionTestUtils.setField(cart, "id", 1L);

        Product product = mock(Product.class);

        CartItem cartItem =
                CartItem.create(
                        cart,
                        product,
                        2
                );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(cartItem));

        when(member.getId())
                .thenReturn(memberId);

        // when
        cartService.deleteCartItem(
                memberId,
                1L
        );

        // then
        assertThat(cartItem.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 상품은 삭제할 수 없습니다.")
    void deleteCartItem_notFound() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = Cart.create(member);

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        when(member.getId())
                .thenReturn(memberId);

        // when & then
        assertThatThrownBy(() ->
                cartService.deleteCartItem(
                        memberId,
                        1L
                ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("다른 회원의 장바구니 상품은 삭제할 수 없습니다.")
    void deleteCartItem_forbidden() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart myCart = Cart.create(member);
        ReflectionTestUtils.setField(myCart, "id", 1L);

        Cart anotherCart = Cart.create(member);
        ReflectionTestUtils.setField(anotherCart, "id", 2L);

        Product product = mock(Product.class);

        CartItem cartItem =
                CartItem.create(
                        anotherCart,
                        product,
                        2
                );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(myCart));

        when(cartItemRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(cartItem));

        when(member.getId())
                .thenReturn(memberId);

        // when & then
        assertThatThrownBy(() ->
                cartService.deleteCartItem(
                        memberId,
                        1L
                ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("장바구니가 없으면 빈 장바구니를 반환합니다.")
    void getCart_emptyCart() {

        // given
        Long memberId = 1L;

        Member member = mock(Member.class);

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.empty());

        // when
        CartResponse response =
                cartService.getCart(memberId);

        // then
        assertThat(response.cartId()).isNull();
        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isEqualTo(0L);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 장바구니를 조회할 수 없습니다.")
    void getCart_memberNotFound() {

        Long memberId = 1L;

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cartService.getCart(memberId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}
