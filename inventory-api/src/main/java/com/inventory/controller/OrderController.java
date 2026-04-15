package com.inventory.controller;

import com.inventory.dto.request.OrderRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "User: place and manage own orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place a new order — checks stock, deducts inventory [USER]")
    public ResponseEntity<ApiResponse.OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest.PlaceOrder request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(request, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "View own order history with pagination and optional status filter [USER]")
    public ResponseEntity<PageResponse<ApiResponse.OrderResponse>> getOrderHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Filter by status: CREATED, CONFIRMED, CANCELLED")
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                PageResponse.of(orderService.getOrderHistory(userDetails.getUsername(), status, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific order by ID [USER]")
    public ResponseEntity<ApiResponse.OrderResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getOrderById(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm a CREATED order [USER]")
    public ResponseEntity<ApiResponse.OrderResponse> confirmOrder(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.confirmOrder(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a CREATED order — restores stock [USER]")
    public ResponseEntity<ApiResponse.OrderResponse> cancelOrder(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.cancelOrder(id, userDetails.getUsername()));
    }
}
