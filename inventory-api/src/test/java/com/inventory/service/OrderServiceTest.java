package com.inventory.service;

import com.inventory.model.*;
import com.inventory.dto.request.OrderRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    @InjectMocks OrderServiceImpl orderService;

    private User user;
    private Product product;

    @BeforeEach
    void setup() {
        user = new User("john", "john@test.com", "hashed", Role.ROLE_USER);
        user.setId("user-001");

        product = new Product("Widget", "A widget", 25.00, 20);
        product.setId("prod-001");
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_success_deductsStock() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        Order savedOrder = new Order("user-001", "john",
                List.of(new OrderItem("prod-001", "Widget", 5, 25.00)));
        savedOrder.setId("order-001");
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.save(any())).thenReturn(savedOrder);

        OrderRequest.PlaceOrder req = buildOrderRequest("prod-001", 5);
        ApiResponse.OrderResponse resp = orderService.placeOrder(req, "john");

        assertThat(resp.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(resp.getTotalAmount()).isEqualTo(125.00);
        // Stock should have been deducted to 15
        verify(productRepository).save(argThat(p -> p.getStockQuantity() == 15));
    }

    @Test
    void placeOrder_insufficientStock_throws() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));

        OrderRequest.PlaceOrder req = buildOrderRequest("prod-001", 999);

        assertThatThrownBy(() -> orderService.placeOrder(req, "john"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Widget");
    }

    @Test
    void placeOrder_inactiveProduct_throws() {
        product.setActive(false);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(buildOrderRequest("prod-001", 1), "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void placeOrder_unknownProduct_throws() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(productRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(buildOrderRequest("bad-id", 1), "john"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── confirmOrder ──────────────────────────────────────────────────────────

    @Test
    void confirmOrder_createdOrder_success() {
        Order order = buildOrder(OrderStatus.CREATED);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId("order-001", "user-001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ApiResponse.OrderResponse resp = orderService.confirmOrder("order-001", "john");

        assertThat(resp.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirmOrder_nonCreatedOrder_throwsBadRequest() {
        Order order = buildOrder(OrderStatus.CONFIRMED);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId("order-001", "user-001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder("order-001", "john"))
                .isInstanceOf(BadRequestException.class);
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    void cancelOrder_restoresStock() {
        OrderItem item = new OrderItem("prod-001", "Widget", 5, 25.00);
        Order order = new Order("user-001", "john", List.of(item));
        order.setId("order-001");
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId("order-001", "user-001")).thenReturn(Optional.of(order));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ApiResponse.OrderResponse resp = orderService.cancelOrder("order-001", "john");

        assertThat(resp.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // Stock restored: 20 + 5 = 25
        verify(productRepository).save(argThat(p -> p.getStockQuantity() == 25));
    }

    @Test
    void cancelOrder_confirmedOrder_throwsBadRequest() {
        Order order = buildOrder(OrderStatus.CONFIRMED);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId("order-001", "user-001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-001", "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Confirmed");
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsBadRequest() {
        Order order = buildOrder(OrderStatus.CANCELLED);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId("order-001", "user-001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-001", "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    // ── getOrderHistory ───────────────────────────────────────────────────────

    @Test
    void getOrderHistory_invalidStatus_throwsBadRequest() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.getOrderHistory("john", "INVALID", 0, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getOrderHistory_noFilter_returnsAll() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(eq("user-001"), any()))
                .thenReturn(new PageImpl<>(List.of(buildOrder(OrderStatus.CREATED))));

        var result = orderService.getOrderHistory("john", null, 0, 10);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrderRequest.PlaceOrder buildOrderRequest(String productId, int qty) {
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(qty);
        OrderRequest.PlaceOrder req = new OrderRequest.PlaceOrder();
        req.setItems(List.of(item));
        return req;
    }

    private Order buildOrder(OrderStatus status) {
        OrderItem item = new OrderItem("prod-001", "Widget", 2, 25.00);
        Order o = new Order("user-001", "john", List.of(item));
        o.setId("order-001");
        o.setStatus(status);
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        return o;
    }
}
