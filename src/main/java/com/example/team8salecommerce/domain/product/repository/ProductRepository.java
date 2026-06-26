package com.example.team8salecommerce.domain.product.repository;

import com.example.team8salecommerce.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
select p
from Product p
join fetch p.category
where p.id = :id
  and p.isDeleted = false
""")
    Optional<Product> findByIdWithCategory(Long id);

    Page<Product> findByIsDeletedFalse(Pageable pageable);
}