package com.example.team8salecommerce.domain.product.repository;

import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.entity.QProduct;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> searchProducts(String keyword, Long categoryId, Long minPrice, Long maxPrice, Pageable pageable) {
        QProduct product = QProduct.product;

        List<Product> content = queryFactory
                .selectFrom(product)
                .leftJoin(product.category).fetchJoin()
                .where(
                        keywordContains(keyword),
                        categoryEq(categoryId),
                        priceGoe(minPrice),
                        priceLoe(maxPrice),
                        product.isDeleted.eq(false)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(product.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .leftJoin(product.category)
                .where(
                        keywordContains(keyword),
                        categoryEq(categoryId),
                        priceGoe(minPrice),
                        priceLoe(maxPrice),
                        product.isDeleted.eq(false)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        QProduct product = QProduct.product;
        return product.name.containsIgnoreCase(keyword)
                .or(product.brand.containsIgnoreCase(keyword))
                .or(product.category.name.containsIgnoreCase(keyword));
    }

    private BooleanExpression categoryEq(Long categoryId) {
        return categoryId != null ? QProduct.product.category.id.eq(categoryId) : null;
    }

    private BooleanExpression priceGoe(Long minPrice) {
        return minPrice != null ? QProduct.product.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(Long maxPrice) {
        return maxPrice != null ? QProduct.product.price.loe(maxPrice) : null;
    }
}
