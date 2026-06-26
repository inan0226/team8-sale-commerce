package com.example.team8salecommerce.domain.category;

import com.example.team8salecommerce.domain.category.dto.CategoryProductResponse;
import com.example.team8salecommerce.domain.category.exception.CategoryException;
import com.example.team8salecommerce.domain.category.service.CategoryService;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리별 상품 목록 조회 성공")
    void getCategoryProducts_success() throws Exception {
        CategoryProductResponse response = new CategoryProductResponse(List.of());

        when(categoryService.getCategoryProducts(eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(get("/categories/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리별 상품 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID 조회 시 404 NOT FOUND")
    void getCategoryProducts_notFound() throws Exception {
        when(categoryService.getCategoryProducts(eq(999L), any()))
                .thenThrow(new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(get("/categories/999/products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("카테고리를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("카테고리 ID가 0이면 400 Bad Request")
    void getCategoryProducts_idZero() throws Exception {
        mockMvc.perform(get("/categories/0/products"))
                .andExpect(status().isBadRequest());
    }
}
