package com.inventory.service;

import com.inventory.dto.request.OrderRequest;
import com.inventory.dto.response.ApiResponse;
import org.springframework.data.domain.Page;

public interface OrderService {
    ApiResponse.OrderResponse placeOrder(OrderRequest.PlaceOrder request, String username);
    ApiResponse.OrderResponse confirmOrder(String orderId, String username);
    ApiResponse.OrderResponse cancelOrder(String orderId, String username);
    ApiResponse.OrderResponse getOrderById(String orderId, String username);
    Page<ApiResponse.OrderResponse> getOrderHistory(String username, String status, int page, int size);
}
