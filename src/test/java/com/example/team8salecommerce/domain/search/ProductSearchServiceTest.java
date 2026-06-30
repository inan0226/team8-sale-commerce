package com.example.team8salecommerce.domain.search;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.fixture.CategoryFixture;
import com.example.team8salecommerce.domain.fixture.ProductFixture;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.domain.search.dto.ProductSearchCache;
import com.example.team8salecommerce.domain.search.service.ProductSearchService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @InjectMocks
    private ProductSearchService productSearchService;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 동적 검색 성공")
    void searchProducts_success() {
        // given
        Category category = CategoryFixture.전자제품();
        Product product = ProductFixture.상품(category);
        ReflectionTestUtils.setField(product, "id", 10L);

        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);

        when(productRepository.searchProducts(anyString(), anyLong(), anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        ProductSearchCache response = productSearchService.searchProducts(
                "에어팟", 1L, 100000L, 500000L, 0, 20
        );

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(response.getContent().get(0).getName()).isEqualTo("상품명");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);

        verify(productRepository).searchProducts(anyString(), anyLong(), anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("검색어가 카테고리명일 때도 정상적으로 검색 서비스를 호출하여 결과를 반환한다")
    void searchProducts_byCategoryKeywordSuccess() {
        Category category = CategoryFixture.전자제품();
        Product product = ProductFixture.상품(category);
        ReflectionTestUtils.setField(product, "id", 20L);

        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);

        when(productRepository.searchProducts(eq("전자제품"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);

        ProductSearchCache response = productSearchService.searchProducts(
                "전자제품", null, null, null, 0, 20
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(20L);

        verify(productRepository).searchProducts(eq("전자제품"), any(), any(), any(), any(Pageable.class));
    }
}
