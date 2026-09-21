package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.CreateOrderRequest;
import com.hoadiemcat.dto.request.OrderItemRequest;
import com.hoadiemcat.dto.response.OrderItemResponse;
import com.hoadiemcat.dto.response.OrderResponse;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.entity.Order;
import com.hoadiemcat.entity.OrderItem;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.OrderItemStatus;
import com.hoadiemcat.entity.enums.OrderStatus;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.repository.OrderItemRepository;
import com.hoadiemcat.repository.OrderRepository;
import com.hoadiemcat.repository.RestaurantTableRepository;
import com.hoadiemcat.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String rawTable = request.getTableNumber();
        String normalizedTable = rawTable.trim().toUpperCase();
        if (normalizedTable.startsWith("BÀN ") || normalizedTable.startsWith("BAN ")) {
            normalizedTable = "B" + normalizedTable.replaceAll("[^0-9]", "");
        } else if (!normalizedTable.startsWith("B") && !normalizedTable.startsWith("VIP")) {
            try {
                normalizedTable = "B" + String.format("%02d", Integer.parseInt(normalizedTable));
            } catch (Exception e) {
                // Giữ nguyên nếu không parse được
            }
        }

        final String searchTableNumber = normalizedTable;
        RestaurantTable table = restaurantTableRepository.findByTableNumber(searchTableNumber)
                .orElseGet(() -> {
                    return restaurantTableRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "tableNumber", searchTableNumber));
                });

        String sessionToken = request.getSessionToken();
        if (sessionToken == null || sessionToken.isBlank()) {
            sessionToken = table.getCurrentSessionToken() != null ? table.getCurrentSessionToken() : "SESSION_" + table.getTableNumber();
        }

        // Tính round number
        List<Order> existingOrders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(table);
        int roundNumber = existingOrders.size() + 1;

        Order order = Order.builder()
                .restaurantTable(table)
                .sessionToken(sessionToken)
                .roundNumber(roundNumber)
                .status(OrderStatus.COOKING)
                .note(request.getNote())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = null;
            if (itemReq.getMenuItemId() != null) {
                menuItem = menuItemRepository.findById(itemReq.getMenuItemId()).orElse(null);
            }
            if (menuItem == null && itemReq.getName() != null) {
                menuItem = menuItemRepository.findByName(itemReq.getName()).orElse(null);
            }
            if (menuItem == null) {
                menuItem = menuItemRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "name", itemReq.getName()));
            }

            BigDecimal itemPrice = itemReq.getPrice() != null ? itemReq.getPrice() : menuItem.getPrice();
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            BigDecimal subtotal = itemPrice.multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .price(itemPrice)
                    .quantity(qty)
                    .totalPrice(subtotal)
                    .status(OrderItemStatus.COOKING)
                    .note(itemReq.getNote())
                    .build();

            order.addOrderItem(orderItem);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        OrderResponse response = mapToResponse(saved);

        // Bắn WebSocket thông báo tới Bếp KDS và Màn hình Bàn
        try {
            messagingTemplate.convertAndSend("/topic/kitchen/orders", response);
            messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/status", response);
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket order: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getTableOrders(String tableNumber) {
        return orderRepository.findByTableNumberOrderByCreatedAtAsc(tableNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getKitchenQueue() {
        return orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(OrderStatus.PENDING, OrderStatus.COOKING))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderItemResponse updateOrderItemStatus(Long orderItemId, OrderItemStatus status) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));

        item.setStatus(status);
        if (status == OrderItemStatus.SERVED) {
            item.setServedAt(LocalDateTime.now());
        } else if (status == OrderItemStatus.DELIVERED) {
            item.setDeliveredAt(LocalDateTime.now());
            if (item.getServedAt() == null) {
                item.setServedAt(LocalDateTime.now());
            }
        }

        OrderItem savedItem = orderItemRepository.save(item);
        Order order = item.getOrder();

        boolean allDelivered = order.getOrderItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.DELIVERED || i.getStatus() == OrderItemStatus.CANCELLED);
        if (allDelivered) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        }

        OrderItemResponse response = mapItemToResponse(savedItem);

        // Broadcast qua WebSocket tới KDS, Waiter và Table
        try {
            String eventType = (status == OrderItemStatus.DELIVERED)
                    ? "WAITER_ITEM_DELIVERED"
                    : "KITCHEN_ITEM_STATUS_TOGGLED";

            Map<String, Object> event = Map.of(
                    "type", eventType,
                    "orderId", order.getId(),
                    "itemId", item.getId(),
                    "nextStatus", status.name(),
                    "affectedItemName", item.getMenuItem() != null ? item.getMenuItem().getName() : "Món",
                    "affectedTableCode", order.getRestaurantTable() != null ? order.getRestaurantTable().getName() : "BÀN"
            );
            messagingTemplate.convertAndSend("/topic/kitchen/orders", event);
            messagingTemplate.convertAndSend("/topic/waiter/orders", event);
            if (order.getRestaurantTable() != null) {
                messagingTemplate.convertAndSend("/topic/table/" + order.getRestaurantTable().getTableNumber() + "/status", event);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket cập nhật trạng thái món: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getWaiterOrders() {
        return orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(OrderStatus.PENDING, OrderStatus.COOKING))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderItemResponse deliverOrderItem(Long orderItemId) {
        return updateOrderItemStatus(orderItemId, OrderItemStatus.DELIVERED);
    }

    @Override
    @Transactional
    public OrderResponse deliverAllOrderItems(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        LocalDateTime now = LocalDateTime.now();
        for (OrderItem item : order.getOrderItems()) {
            if (item.getStatus() == OrderItemStatus.SERVED || item.getStatus() == OrderItemStatus.COOKING) {
                item.setStatus(OrderItemStatus.DELIVERED);
                if (item.getServedAt() == null) item.setServedAt(now);
                item.setDeliveredAt(now);
            }
        }
        order.setStatus(OrderStatus.COMPLETED);
        Order saved = orderRepository.save(order);
        OrderResponse response = mapToResponse(saved);

        try {
            Map<String, Object> event = Map.of(
                    "type", "WAITER_ALL_ITEMS_DELIVERED",
                    "orderId", order.getId(),
                    "affectedTable", order.getRestaurantTable() != null ? order.getRestaurantTable().getName() : "BÀN"
            );
            messagingTemplate.convertAndSend("/topic/waiter/orders", event);
            messagingTemplate.convertAndSend("/topic/kitchen/orders", event);
            if (order.getRestaurantTable() != null) {
                messagingTemplate.convertAndSend("/topic/table/" + order.getRestaurantTable().getTableNumber() + "/status", event);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket deliverAllOrderItems: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(status);
        if (status == OrderStatus.COMPLETED) {
            LocalDateTime now = LocalDateTime.now();
            for (OrderItem item : order.getOrderItems()) {
                if (item.getStatus() == OrderItemStatus.COOKING || item.getStatus() == OrderItemStatus.SERVED) {
                    item.setStatus(OrderItemStatus.DELIVERED);
                    if (item.getServedAt() == null) item.setServedAt(now);
                    item.setDeliveredAt(now);
                }
            }
        }

        Order saved = orderRepository.save(order);
        OrderResponse response = mapToResponse(saved);

        try {
            messagingTemplate.convertAndSend("/topic/kitchen/orders", response);
            messagingTemplate.convertAndSend("/topic/waiter/orders", response);
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket order status: {}", e.getMessage());
        }

        return response;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .tableCode(order.getRestaurantTable() != null ? order.getRestaurantTable().getTableNumber() : "")
                .tableName(order.getRestaurantTable() != null ? order.getRestaurantTable().getName() : "")
                .sessionToken(order.getSessionToken())
                .roundNumber(order.getRoundNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem() != null ? item.getMenuItem().getId() : null)
                .name(item.getMenuItem() != null ? item.getMenuItem().getName() : "")
                .imageUrl(item.getMenuItem() != null ? item.getMenuItem().getImageUrl() : "")
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .note(item.getNote())
                .status(item.getStatus())
                .servedAt(item.getServedAt())
                .deliveredAt(item.getDeliveredAt())
                .build();
    }
}
