package com.example.team8salecommerce.domain.category;

import com.example.team8salecommerce.domain.category.dto.CategoryProductResponse;
import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.category.exception.CategoryException;
import com.example.team8salecommerce.domain.category.repository.CategoryRepository;
import com.example.team8salecommerce.domain.category.service.CategoryService;
import com.example.team8salecommerce.domain.fixture.CategoryFixture;
import com.example.team8salecommerce.domain.fixture.ProductFixture;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("카테고리별 상품 목록 조회 성공")
    void getCategoryProducts_success() {
        // given
        Long categoryId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        Category category = CategoryFixture.전자제품();
        ReflectionTestUtils.setField(category, "id", categoryId);

        Product product = ProductFixture.상품(category);
        ReflectionTestUtils.setField(product, "id", 100L);

        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.findByCategoryIdAndIsDeletedFalse(categoryId, pageable)).thenReturn(productPage);

        // when
        CategoryProductResponse response = categoryService.getCategoryProducts(categoryId, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(100L);
        assertThat(response.content().get(0).name()).isEqualTo("상품명");

        verify(categoryRepository).existsById(categoryId);
        verify(productRepository).findByCategoryIdAndIsDeletedFalse(categoryId, pageable);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID 조회 시 예외 발생")
    void getCategoryProducts_categoryNotFound() {
        // given
        Long categoryId = 999L;
        Pageable pageable = PageRequest.of(0, 20);

        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> categoryService.getCategoryProducts(categoryId, pageable))
                .isInstanceOf(CategoryException.class);

        verify(categoryRepository).existsById(categoryId);
        verifyNoInteractions(productRepository);
    }
}
