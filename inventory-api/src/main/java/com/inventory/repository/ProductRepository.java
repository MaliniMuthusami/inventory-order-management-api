package com.inventory.repository;

import com.inventory.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // All active products — explicit @Query ensures Pageable sort is always honoured
    @Query("{ 'active': true }")
    Page<Product> findAllActive(Pageable pageable);

    // Search by name (case-insensitive regex)
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'active': true }")
    Page<Product> searchByName(String nameRegex, Pageable pageable);

    // Filter by category ID
    @Query("{ 'categoryIds': ?0, 'active': true }")
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    // Low-stock products (active only)
    @Query("{ 'stockQuantity': { $lte: ?0 }, 'active': true }")
    Page<Product> findLowStock(int threshold, Pageable pageable);
}
