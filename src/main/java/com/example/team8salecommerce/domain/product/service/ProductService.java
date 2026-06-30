package com.example.team8salecommerce.domain.product.service;

import com.example.team8salecommerce.domain.product.dto.ProductDetailResponse;
import com.example.team8salecommerce.domain.product.dto.ProductPageResponse;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.exception.ProductException;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import com.example.team8salecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(Pageable pageable) {
        log.info("상품 목록 조회 시작");
        Pageable resolvedPageable = convertPageable(pageable);
        Page<Product> productPage = productRepository.findByIsDeletedFalse(resolvedPageable);
        log.info("상품 목록 조회 완료");
        return ProductPageResponse.from(productPage);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        Pageable resolvedPageable = convertPageable(pageable);
        return productRepository.findByCategoryIdAndIsDeletedFalse(categoryId, resolvedPageable);
    }

    private Pageable convertPageable(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), 100);
        Sort sort = pageable.getSort();

        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "id");
        } else {
            List<Sort.Order> orders = new ArrayList<>();
            for (Sort.Order order : sort) {
                String property = order.getProperty().toUpperCase();

                switch (property) {
                    case "PRICE_ASC":
                        orders.add(new Sort.Order(Sort.Direction.ASC, "price"));
                        break;
                    case "PRICE_DESC":
                        orders.add(new Sort.Order(Sort.Direction.DESC, "price"));
                        break;
                    case "NAME_ASC":
                        orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
                        break;
                    case "NAME_DESC":
                        orders.add(new Sort.Order(Sort.Direction.DESC, "name"));
                        break;
                    case "LATEST":
                        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
                        break;
                    default:
                        orders.add(order);
                        break;
                }
            }
            sort = Sort.by(orders);
        }

        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
