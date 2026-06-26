package com.example.team8salecommerce.domain.product;

import com.example.team8salecommerce.domain.product.exception.ProductException;
import com.example.team8salecommerce.domain.product.service.ProductService;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("삭제된 상품 조회 시 404")
    void product_not_found() throws Exception {

        when(productService.getProductDetail(999L))
                .thenThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 ID가 0이면 400 Bad Request")
    void product_id_zero() throws Exception {

        mockMvc.perform(get("/products/0"))
                .andExpect(status().isBadRequest());
    }
}