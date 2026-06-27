package com.example.team8salecommerce.domain.search.service;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.product.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchSpecification {

    public static Specification<Product> searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 삭제 안 된 상품만 조회
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // 키워드 검색 (상품명, 브랜드명, 카테고리명 중 하나라도 매칭)
            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.trim() + "%";
                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);

                Predicate nameLike = cb.like(root.get("name"), likePattern);
                Predicate brandLike = cb.like(root.get("brand"), likePattern);
                Predicate categoryLike = cb.like(categoryJoin.get("name"), likePattern);

                predicates.add(cb.or(nameLike, brandLike, categoryLike));
            }

            // 카테고리 ID 필터링
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 최소 가격 조건
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            // 최대 가격 조건
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
