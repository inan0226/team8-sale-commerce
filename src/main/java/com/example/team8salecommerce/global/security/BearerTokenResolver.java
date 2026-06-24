package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public String resolve(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
