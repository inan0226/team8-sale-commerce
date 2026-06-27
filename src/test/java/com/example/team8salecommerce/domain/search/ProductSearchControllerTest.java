package com.example.team8salecommerce.domain.search;

import com.example.team8salecommerce.domain.search.dto.ProductSearchResponse;
import com.example.team8salecommerce.domain.search.service.ProductSearchService;
import com.example.team8salecommerce.domain.search.service.SearchKeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProductSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSearchService productSearchService;

    @MockitoBean
    private SearchKeywordService searchKeywordService;

    @MockitoBean
    private RedissonClient redissonClient;

    @Test
    @DisplayName("상품 검색 성공")
    void searchProducts_success() throws Exception {
        ProductSearchResponse response = new ProductSearchResponse(List.of(), 0, 20, 1, 0L);

        when(productSearchService.searchProducts(
                anyString(), anyLong(), anyLong(), anyLong(), anyInt(), anyInt()
        )).thenReturn(response);

        mockMvc.perform(get("/search/products")
                        .param("keyword", "에어팟")
                        .param("categoryId", "1")
                        .param("minPrice", "100000")
                        .param("maxPrice", "500000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품 검색 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(searchKeywordService).incrementKeywordCount("에어팟");
    }

    @Test
    @DisplayName("검색 조건이 전혀 없을 때 400 Bad Request 반환")
    void searchProducts_invalidSearchCondition() throws Exception {
        mockMvc.perform(get("/search/products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("검색어, 카테고리, 가격 필터 중 적어도 하나는 입력해야 합니다."));
    }

    @Test
    @DisplayName("최소 가격이 최대 가격보다 클 때 400 Bad Request 반환")
    void searchProducts_invalidPriceRange() throws Exception {
        mockMvc.perform(get("/search/products")
                        .param("keyword", "에어팟")
                        .param("minPrice", "500000")
                        .param("maxPrice", "100000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("최소 가격이 최대 가격보다 클 수 없습니다."));
    }
}
