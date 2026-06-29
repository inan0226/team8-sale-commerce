package com.example.team8salecommerce.domain.search.service;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.product.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchSpecification {

    private static final String IS_DELETED = "isDeleted";
    private static final String CATEGORY = "category";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String BRAND = "brand";
    private static final String PRICE = "price";

    public static Specification<Product> searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 삭제 안 된 상품만 조회
            predicates.add(cb.equal(root.get(IS_DELETED), false));

            // 키워드 검색 (상품명, 브랜드명, 카테고리명 중 하나라도 매칭)
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.trim() + "%";
                Join<Product, Category> categoryJoin = root.join(CATEGORY, JoinType.LEFT);

                Predicate nameLike = cb.like(root.get(NAME), likePattern);
                Predicate brandLike = cb.like(root.get(BRAND), likePattern);
                Predicate categoryLike = cb.like(categoryJoin.get(NAME), likePattern);

                predicates.add(cb.or(nameLike, brandLike, categoryLike));
            }

            // 카테고리 ID 필터링
            if (categoryId != null) {
                predicates.add(cb.equal(root.get(CATEGORY).get(ID), categoryId));
            }

            // 최소 가격 조건
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(PRICE), minPrice));
            }

            // 최대 가격 조건
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(PRICE), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
