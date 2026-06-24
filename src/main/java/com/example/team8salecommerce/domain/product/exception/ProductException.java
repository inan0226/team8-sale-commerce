package com.example.team8salecommerce.domain.product.exception;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

public class ProductException extends CustomException {
    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}