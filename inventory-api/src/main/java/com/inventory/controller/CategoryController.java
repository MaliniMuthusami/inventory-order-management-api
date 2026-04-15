package com.inventory.controller;

import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categories", description = "Admin: manage product categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @Operation(summary = "Create a new category [ADMIN]")
    public ResponseEntity<ApiResponse.CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping
    @Operation(summary = "Get all categories [ADMIN]")
    public ResponseEntity<List<ApiResponse.CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID [ADMIN]")
    public ResponseEntity<ApiResponse.CategoryResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category [ADMIN]")
    public ResponseEntity<ApiResponse.CategoryResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category [ADMIN]")
    public ResponseEntity<ApiResponse.MessageResponse> delete(@PathVariable String id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.MessageResponse.ok("Category deleted successfully"));
    }
}
