package com.inventory.controller;

import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Products", description = "Admin: manage products | User: browse products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ── Admin write endpoints ─────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a product [ADMIN]")
    public ResponseEntity<ApiResponse.ProductResponse> create(
            @Valid @RequestBody ProductRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product [ADMIN]")
    public ResponseEntity<ApiResponse.ProductResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest.Update request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product [ADMIN]")
    public ResponseEntity<ApiResponse.MessageResponse> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.MessageResponse.ok("Product deleted successfully"));
    }

    // ── Read endpoints — accessible by all authenticated users ───────────────

    @GetMapping
    @Operation(summary = "List all active products with pagination and sorting [ALL]")
    public ResponseEntity<PageResponse<ApiResponse.ProductResponse>> getAll(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @Parameter(description = "Sort field: price, name, stockQuantity, createdAt")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "asc or desc")
            @RequestParam(defaultValue = "asc")  String direction) {
        return ResponseEntity.ok(PageResponse.of(productService.getAll(page, size, sortBy, direction)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID [ALL]")
    public ResponseEntity<ApiResponse.ProductResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name (case-insensitive) [ALL]")
    public ResponseEntity<PageResponse<ApiResponse.ProductResponse>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(PageResponse.of(productService.searchByName(name, page, size)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Filter products by category with sorting [ALL]")
    public ResponseEntity<PageResponse<ApiResponse.ProductResponse>> filterByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc")   String direction) {
        return ResponseEntity.ok(PageResponse.of(productService.filterByCategory(categoryId, page, size, sortBy, direction)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "View low-stock products (Admin view of inventory health) [ALL]")
    public ResponseEntity<PageResponse<ApiResponse.ProductResponse>> lowStock(
            @Parameter(description = "Stock threshold — products at or below this level are returned")
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(PageResponse.of(productService.getLowStock(threshold, page, size)));
    }
}
