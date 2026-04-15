package com.inventory.service.impl;

import com.inventory.model.Product;
import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ApiResponse.ProductResponse create(ProductRequest.Create request) {
        validateCategoryIds(request.getCategoryIds());

        Product product = new Product(request.getName(), request.getDescription(),
                request.getPrice(), request.getStockQuantity());
        product.setCategoryIds(request.getCategoryIds());
        productRepository.save(product);
        log.info("Product '{}' created", product.getName());
        return toResponse(product);
    }

    @Override
    public ApiResponse.ProductResponse update(String id, ProductRequest.Update request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getName() != null)          product.setName(request.getName());
        if (request.getDescription() != null)   product.setDescription(request.getDescription());
        if (request.getPrice() != null)         product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getActive() != null)        product.setActive(request.getActive());
        if (request.getCategoryIds() != null) {
            validateCategoryIds(request.getCategoryIds());
            product.setCategoryIds(request.getCategoryIds());
        }

        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product '{}' updated", product.getName());
        return toResponse(product);
    }

    @Override
    public ApiResponse.ProductResponse getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return toResponse(product);
    }

    @Override
    public Page<ApiResponse.ProductResponse> getAll(int page, int size, String sortBy, String direction) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return productRepository.findAllActive(pageable).map(this::toResponse);
    }

    @Override
    public Page<ApiResponse.ProductResponse> searchByName(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.searchByName(name, pageable).map(this::toResponse);
    }

    @Override
    public Page<ApiResponse.ProductResponse> filterByCategory(String categoryId, int page, int size,
                                                               String sortBy, String direction) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return productRepository.findByCategoryId(categoryId, pageable).map(this::toResponse);
    }

    @Override
    public Page<ApiResponse.ProductResponse> getLowStock(int threshold, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("stockQuantity").ascending());
        return productRepository.findLowStock(threshold, pageable).map(this::toResponse);
    }

    @Override
    public void delete(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        productRepository.delete(product);
        log.info("Product '{}' deleted", product.getName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateCategoryIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (String catId : ids) {
            if (!categoryRepository.existsById(catId)) {
                throw new ResourceNotFoundException("Category", catId);
            }
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }

    public ApiResponse.ProductResponse toResponse(Product p) {
        ApiResponse.ProductResponse r = new ApiResponse.ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setPrice(p.getPrice());
        r.setStockQuantity(p.getStockQuantity());
        r.setActive(p.isActive());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());

        List<ApiResponse.CategoryResponse> cats = new ArrayList<>();
        if (p.getCategoryIds() != null) {
            for (String catId : p.getCategoryIds()) {
                categoryRepository.findById(catId).ifPresent(c -> {
                    ApiResponse.CategoryResponse cr = new ApiResponse.CategoryResponse();
                    cr.setId(c.getId());
                    cr.setName(c.getName());
                    cr.setDescription(c.getDescription());
                    cats.add(cr);
                });
            }
        }
        r.setCategories(cats);
        return r;
    }
}
