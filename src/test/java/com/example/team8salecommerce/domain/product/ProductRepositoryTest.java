package com.example.team8salecommerce.domain.product;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.category.repository.CategoryRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("QueryDSL 기반 상품명, 브랜드명, 카테고리명 동적 검색 및 가격 필터링이 실제 DB에서 정상 작동한다")
    void searchProducts_integration_success() {
        // 1. Given: 테스트 데이터 저장
        Category categoryElectronics = Category.create("가전제품");
        categoryRepository.save(categoryElectronics);

        Category categoryFashion = Category.create("패션의류");
        categoryRepository.save(categoryFashion);

        // 가전제품 상품 1 (에어팟, 애플, 150000원)
        Product airpods = Product.create("에어팟 프로", "애플", 150000L, 10, "imageUrl", "설명", categoryElectronics);
        productRepository.save(airpods);

        // 가전제품 상품 2 (갤럭시 버즈, 삼성, 120000원)
        Product galaxyBuds = Product.create("갤럭시 버즈", "삼성", 120000L, 10, "imageUrl", "설명", categoryElectronics);
        productRepository.save(galaxyBuds);

        // 패션의류 상품 3 (니트, 나이키, 80000원, 삭제됨)
        Product nikeKnitDeleted = Product.createDeleted("라운드 니트", "나이키", 80000L, 5, "imageUrl", "설명", categoryFashion);
        productRepository.save(nikeKnitDeleted);

        Pageable pageable = PageRequest.of(0, 10);

        // 2. When & Then: 동적 조건 검증

        // Case A: 상품명 키워드 "에어팟" 검색
        Page<Product> searchByName = productRepository.searchProducts("에어팟", null, null, null, pageable);
        assertThat(searchByName.getContent()).hasSize(1);
        assertThat(searchByName.getContent().get(0).getName()).contains("에어팟");

        // Case B: 브랜드명 키워드 "삼성" 검색
        Page<Product> searchByBrand = productRepository.searchProducts("삼성", null, null, null, pageable);
        assertThat(searchByBrand.getContent()).hasSize(1);
        assertThat(searchByBrand.getContent().get(0).getBrand()).isEqualTo("삼성");

        // Case C: 카테고리명 키워드 "가전" 검색 (카테고리명 포함 검색 동작 검증)
        Page<Product> searchByCategoryName = productRepository.searchProducts("가전", null, null, null, pageable);
        assertThat(searchByCategoryName.getContent()).hasSize(2);

        // Case D: 가격 범위 필터링 (130000원 ~ 200000원)
        Page<Product> searchByPriceRange = productRepository.searchProducts(null, null, 130000L, 200000L, pageable);
        assertThat(searchByPriceRange.getContent()).hasSize(1);
        assertThat(searchByPriceRange.getContent().get(0).getName()).isEqualTo("에어팟 프로");

        // Case E: 카테고리 ID 필터링
        Page<Product> searchByCategoryId = productRepository.searchProducts(null, categoryElectronics.getId(), null, null, pageable);
        assertThat(searchByCategoryId.getContent()).hasSize(2);

        // Case F: 삭제된 상품("나이키")은 검색되지 않아야 함
        Page<Product> searchDeleted = productRepository.searchProducts("나이키", null, null, null, pageable);
        assertThat(searchDeleted.getContent()).isEmpty();
    }
}
