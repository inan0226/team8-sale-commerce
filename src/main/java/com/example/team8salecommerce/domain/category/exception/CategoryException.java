package com.example.team8salecommerce.domain.category.exception;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

public class CategoryException extends CustomException {
    public CategoryException(ErrorCode errorCode) {
        super(errorCode);
    }
}
