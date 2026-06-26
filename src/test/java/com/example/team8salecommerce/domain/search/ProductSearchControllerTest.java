package com.example.team8salecommerce.domain.search;

import com.example.team8salecommerce.domain.search.dto.ProductSearchResponse;
import com.example.team8salecommerce.domain.search.service.ProductSearchService;
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
    private RedissonClient redissonClient;

    @Test
    @DisplayName("상품 검색 성공")
    void searchProducts_success() throws Exception {
        ProductSearchResponse response = new ProductSearchResponse(List.of(), 0, 20, 1);

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
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }
}
