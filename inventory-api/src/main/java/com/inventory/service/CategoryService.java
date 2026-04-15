package com.inventory.service;

import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.response.ApiResponse;

import java.util.List;

public interface CategoryService {
    ApiResponse.CategoryResponse create(CategoryRequest request);
    ApiResponse.CategoryResponse update(String id, CategoryRequest request);
    void delete(String id);
    ApiResponse.CategoryResponse getById(String id);
    List<ApiResponse.CategoryResponse> getAll();
}
