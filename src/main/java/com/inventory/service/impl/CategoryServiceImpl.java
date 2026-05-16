package com.inventory.service.impl;

import com.inventory.model.Category;
import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ApiResponse.CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category '" + request.getName() + "' already exists");
        }
        Category cat = new Category(request.getName(), request.getDescription());
        categoryRepository.save(cat);
        log.info("Category '{}' created", cat.getName());
        return toResponse(cat);
    }

    @Override
    public ApiResponse.CategoryResponse update(String id, CategoryRequest request) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!cat.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category '" + request.getName() + "' already exists");
        }

        cat.setName(request.getName());
        cat.setDescription(request.getDescription());
        cat.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(cat);
        log.info("Category '{}' updated", cat.getName());
        return toResponse(cat);
    }

    @Override
    public void delete(String id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        categoryRepository.delete(cat);
        log.info("Category '{}' deleted", cat.getName());
    }

    @Override
    public ApiResponse.CategoryResponse getById(String id) {
        return toResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id)));
    }

    @Override
    public List<ApiResponse.CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ApiResponse.CategoryResponse toResponse(Category c) {
        ApiResponse.CategoryResponse r = new ApiResponse.CategoryResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setDescription(c.getDescription());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        return r;
    }
}
