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

        ProductDetailResponse response =
                productService.getProductDetail(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(product.getName());

        verify(productRepository)
                .findByIdWithCategory(1L);
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
}