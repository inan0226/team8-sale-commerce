package com.example.team8salecommerce.domain.product;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.category.repository.CategoryRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {

        category = categoryRepository.save(
                new Category("전자제품")
        );

        product = productRepository.save(
                new Product(
                        "상품명",
                        "브랜드",
                        1000L,
                        10,
                        "img.jpg",
                        "설명",
                        false,
                        0,
                        category
                )
        );
    }

    @Test
    void 상품_상세조회_성공() throws Exception {

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(product.getId()));
    }

    @Test
    void 존재하지_않는_상품_조회시_404() throws Exception {

        mockMvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("상품을 찾을 수 없습니다."));
    }

    @Test
    void 삭제된_상품_조회시_404() throws Exception {

        Product deletedProduct = productRepository.save(
                new Product(
                        "상품명2",
                        "브랜드",
                        1000L,
                        10,
                        "img.jpg",
                        "설명",
                        true,   // isDeleted = true
                        0,
                        category
                )
        );

        mockMvc.perform(get("/products/" + deletedProduct.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 상품ID가_0이면_400() throws Exception {

        mockMvc.perform(get("/products/0"))
                .andExpect(status().isBadRequest());
    }
}