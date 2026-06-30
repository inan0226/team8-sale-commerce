package com.example.team8salecommerce.domain.product;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.fixture.CategoryFixture;
import com.example.team8salecommerce.domain.fixture.ProductFixture;
import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.exception.ProductException;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_success() {

        Category category = CategoryFixture.전자제품();
        Product product = ProductFixture.상품(category);

        ReflectionTestUtils.setField(product, "id", 1L);

        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(product));

        ProductDetailResponse expected = ProductFixture.상품상세응답(product);
        ProductDetailResponse response = productService.getProductDetail(1L);

        assertThat(response).isEqualTo(expected);

        verify(productRepository).findByIdWithCategory(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    void getProductDetail_notFound() {

        when(productRepository.findByIdWithCategory(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.getProductDetail(999L))
                .isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("삭제된 상품은 조회되지 않는다")
    void getProductDetail_deletedProduct() {

        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.getProductDetail(1L))
                .isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("상품 ID가 0이면 조회 로직 수행 전에 예외 상황으로 처리된다")
    void productId_zero_case() {

        when(productRepository.findByIdWithCategory(0L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.getProductDetail(0L))
                .isInstanceOf(ProductException.class);

        verify(productRepository).findByIdWithCategory(0L);
    }

    @Test
    @DisplayName("상품 목록 조회 시 기존 sort 파라미터가 엔티티 실제 필드로 정상 변환되고 size가 100으로 제한된다")
    void getProducts_sortAndSizeValidation() {
        Pageable pageable = PageRequest.of(0, 150, Sort.by("PRICE_ASC"));
        Page<Product> productPage = new PageImpl<>(List.of());

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findByIsDeletedFalse(pageableCaptor.capture())).thenReturn(productPage);

        productService.getProducts(pageable);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageSize()).isEqualTo(100);
        assertThat(captured.getSort().getOrderFor("price")).isNotNull();
        assertThat(captured.getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("허용되지 않은 필드로 정렬을 요청하면 기본 정렬(createdAt, DESC)로 안전하게 대체된다")
    void getProducts_invalidSortPropertyValidation() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("invalidFieldName"));
        Page<Product> productPage = new PageImpl<>(List.of());

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findByIsDeletedFalse(pageableCaptor.capture())).thenReturn(productPage);

        productService.getProducts(pageable);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captured.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(captured.getSort().getOrderFor("invalidFieldName")).isNull();
    }

    @Test
    @DisplayName("LATEST 정렬 값으로 요청하면 createdAt DESC 정렬로 정상 변환된다")
    void getProducts_latestSortMappingValidation() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("LATEST"));
        Page<Product> productPage = new PageImpl<>(List.of());

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findByIsDeletedFalse(pageableCaptor.capture())).thenReturn(productPage);

        productService.getProducts(pageable);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captured.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}