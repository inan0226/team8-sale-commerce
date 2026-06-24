package com.example.team8salecommerce.domain.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 상품_상세조회_성공() throws Exception {

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
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

        mockMvc.perform(get("/products/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("상품을 찾을 수 없습니다."));
    }

    @Test
    void 상품ID가_0이면_400() throws Exception {

        mockMvc.perform(get("/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("상품 ID는 1 이상이어야 합니다."));
    }
}