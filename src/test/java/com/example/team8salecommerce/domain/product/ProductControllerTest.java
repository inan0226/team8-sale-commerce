package com.example.team8salecommerce.domain.product;

import com.example.team8salecommerce.domain.product.service.ProductService;
import com.example.team8salecommerce.domain.product.exception.ProductException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private com.example.team8salecommerce.domain.product.controller.ProductController productController;

    @Test
    @DisplayName("삭제된 상품 조회 시 404 예외")
    void product_not_found() {

        when(productService.getProductDetail(999L))
                .thenThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() ->
                productController.getProductDetail(999L)
        ).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("상품 ID 0 처리 검증")
    void product_id_zero() {

        when(productService.getProductDetail(0L))
                .thenThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() ->
                productController.getProductDetail(0L)
        ).isInstanceOf(ProductException.class);
    }
}