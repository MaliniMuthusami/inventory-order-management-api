package com.inventory.service.impl;

import com.inventory.model.*;
import com.inventory.dto.request.OrderRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse.OrderResponse placeOrder(OrderRequest.PlaceOrder request, String username) {
        User user = getUser(username);

        // ── 1. Validate all items before mutating anything ────────────────────
        List<OrderItem> orderItems = new ArrayList<>();
        List<Product> productsToUpdate = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            if (!product.isActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is not available");
            }
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(product.getName(),
                        itemReq.getQuantity(), product.getStockQuantity());
            }

            orderItems.add(new OrderItem(product.getId(), product.getName(),
                    itemReq.getQuantity(), product.getPrice()));
            productsToUpdate.add(product);
        }

        // ── 2. Deduct stock after all checks pass ─────────────────────────────
        for (int i = 0; i < productsToUpdate.size(); i++) {
            Product p = productsToUpdate.get(i);
            int qty = request.getItems().get(i).getQuantity();
            p.setStockQuantity(p.getStockQuantity() - qty);
            p.setUpdatedAt(LocalDateTime.now());
            productRepository.save(p);
            log.info("Stock reduced for '{}': -{} units (remaining: {})", p.getName(), qty, p.getStockQuantity());
        }

        // ── 3. Persist order ──────────────────────────────────────────────────
        Order order = new Order(user.getId(), user.getUsername(), orderItems);
        orderRepository.save(order);
        log.info("Order '{}' placed by '{}'", order.getId(), username);
        return toResponse(order);
    }

    @Override
    public ApiResponse.OrderResponse confirmOrder(String orderId, String username) {
        Order order = getOwnedOrder(orderId, username);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BadRequestException("Only CREATED orders can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order '{}' confirmed by '{}'", orderId, username);
        return toResponse(order);
    }

    @Override
    public ApiResponse.OrderResponse cancelOrder(String orderId, String username) {
        Order order = getOwnedOrder(orderId, username);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new BadRequestException("Confirmed orders cannot be cancelled");
        }

        // ── Restore stock ─────────────────────────────────────────────────────
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
                log.info("Stock restored for '{}': +{} units (now: {})",
                        product.getName(), item.getQuantity(), product.getStockQuantity());
            });
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order '{}' cancelled by '{}'", orderId, username);
        return toResponse(order);
    }

    @Override
    public ApiResponse.OrderResponse getOrderById(String orderId, String username) {
        return toResponse(getOwnedOrder(orderId, username));
    }

    @Override
    public Page<ApiResponse.OrderResponse> getOrderHistory(String username, String status, int page, int size) {
        User user = getUser(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                return orderRepository.findByUserIdAndStatus(user.getId(), orderStatus, pageable)
                        .map(this::toResponse);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status. Use CREATED, CONFIRMED, or CANCELLED");
            }
        }
        return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private Order getOwnedOrder(String orderId, String username) {
        User user = getUser(username);
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private ApiResponse.OrderResponse toResponse(Order o) {
        ApiResponse.OrderResponse r = new ApiResponse.OrderResponse();
        r.setId(o.getId());
        r.setUserId(o.getUserId());
        r.setUsername(o.getUsername());
        r.setTotalAmount(o.getTotalAmount());
        r.setStatus(o.getStatus());
        r.setCreatedAt(o.getCreatedAt());
        r.setUpdatedAt(o.getUpdatedAt());

        List<ApiResponse.OrderItemResponse> items = o.getItems().stream().map(item -> {
            ApiResponse.OrderItemResponse ir = new ApiResponse.OrderItemResponse();
            ir.setProductId(item.getProductId());
            ir.setProductName(item.getProductName());
            ir.setQuantity(item.getQuantity());
            ir.setUnitPrice(item.getUnitPrice());
            ir.setSubtotal(item.getSubtotal());
            return ir;
        }).collect(Collectors.toList());

        r.setItems(items);
        return r;
    }
}
