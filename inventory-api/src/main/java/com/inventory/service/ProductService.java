package com.inventory.service;

import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.response.ApiResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
    ApiResponse.ProductResponse create(ProductRequest.Create request);
    ApiResponse.ProductResponse update(String id, ProductRequest.Update request);
    ApiResponse.ProductResponse getById(String id);
    Page<ApiResponse.ProductResponse> getAll(int page, int size, String sortBy, String direction);
    Page<ApiResponse.ProductResponse> searchByName(String name, int page, int size);
    Page<ApiResponse.ProductResponse> filterByCategory(String categoryId, int page, int size, String sortBy, String direction);
    Page<ApiResponse.ProductResponse> getLowStock(int threshold, int page, int size);
    void delete(String id);
}
