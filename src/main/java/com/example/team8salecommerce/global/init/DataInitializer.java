package com.example.team8salecommerce.global.init;

import com.example.team8salecommerce.domain.category.entity.Category;
import com.example.team8salecommerce.domain.category.repository.CategoryRepository;
import com.example.team8salecommerce.domain.product.entity.Product;
import com.example.team8salecommerce.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        Category c = categoryRepository.save(Category.create("노트북"));

        productRepository.save(Product.create(
                "테스트",
                "Logitech",
                10000L,
                10,
                "img.jpg",
                "설명",
                c
        ));
    }
}