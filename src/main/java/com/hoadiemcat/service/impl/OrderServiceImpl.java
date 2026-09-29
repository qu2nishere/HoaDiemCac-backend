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
import com.hoadiemcat.entity.enums.TableStatus;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private String normalizeTableNumber(String rawTable) {
        if (rawTable == null || rawTable.isBlank()) return "B01";
        String normalizedTable = rawTable.trim().toUpperCase();
        if (normalizedTable.startsWith("VIP")) {
            String digits = normalizedTable.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? normalizedTable : "VIP" + digits;
        }
        if (normalizedTable.startsWith("BÀN ") || normalizedTable.startsWith("BAN ")) {
            String digits = normalizedTable.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return "B" + String.format("%02d", Integer.parseInt(digits));
                } catch (Exception ignored) {}
            }
            return "B" + digits;
        }
        if (normalizedTable.startsWith("B")) {
            String digits = normalizedTable.substring(1).trim();
            if (digits.matches("^\\d+$")) {
                try {
                    return "B" + String.format("%02d", Integer.parseInt(digits));
                } catch (Exception ignored) {}
            }
            return normalizedTable;
        }
        if (normalizedTable.matches("^\\d+$")) {
            try {
                return "B" + String.format("%02d", Integer.parseInt(normalizedTable));
            } catch (Exception ignored) {}
        }
        return normalizedTable;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String rawTable = request.getTableNumber();
        String searchTableNumber = normalizeTableNumber(rawTable);

        RestaurantTable table = restaurantTableRepository.findByTableNumber(searchTableNumber)
                .or(() -> restaurantTableRepository.findByName(searchTableNumber))
                .or(() -> restaurantTableRepository.findByName(rawTable.trim()))
                .orElseGet(() -> {
                    return restaurantTableRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "tableNumber", searchTableNumber));
                });

        // BẢO VỆ DOANH THU CỤM BÀN: Nếu đặt món vào Bàn phụ, luôn gắn đơn hàng vào Bàn chính (Master)
        if (table.isLinked()) {
            log.info("Đặt món cho bàn phụ {}, tự động chuyển quyền sở hữu đơn hàng về Bàn chính {}",
                    table.getTableNumber(), table.getMasterTable() != null ? table.getMasterTable().getTableNumber() : "N/A");
            table = table.getEffectiveTable();
        }

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

        // Bắn WebSocket thông báo tới Bếp KDS và Màn hình Bàn (hỗ trợ cả tableNumber và name)
        try {
            messagingTemplate.convertAndSend("/topic/kitchen/orders", response);
            if (table.getTableNumber() != null) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/status", response);
            }
            if (table.getName() != null && !table.getName().equals(table.getTableNumber())) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getName() + "/status", response);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket order: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getTableOrders(String tableNumber) {
        String normalized = normalizeTableNumber(tableNumber);
        RestaurantTable table = restaurantTableRepository.findByTableNumber(normalized)
                .or(() -> restaurantTableRepository.findByName(normalized))
                .orElse(null);

        if (table != null && table.getCurrentSessionToken() != null && table.getStatus() == TableStatus.OCCUPIED) {
            return orderRepository.findByTableNumberAndSessionTokenOrderByCreatedAtAsc(normalized, table.getCurrentSessionToken())
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return orderRepository.findByTableNumberOrderByCreatedAtAsc(normalized)
                .stream()
                .filter(o -> o.getStatus() != OrderStatus.COMPLETED && o.getStatus() != OrderStatus.CANCELLED)
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
                String tNum = order.getRestaurantTable().getTableNumber();
                String tName = order.getRestaurantTable().getName();
                if (tNum != null) {
                    messagingTemplate.convertAndSend("/topic/table/" + tNum + "/status", event);
                }
                if (tName != null && !tName.equals(tNum)) {
                    messagingTemplate.convertAndSend("/topic/table/" + tName + "/status", event);
                }
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
                String tNum = order.getRestaurantTable().getTableNumber();
                String tName = order.getRestaurantTable().getName();
                if (tNum != null) {
                    messagingTemplate.convertAndSend("/topic/table/" + tNum + "/status", event);
                }
                if (tName != null && !tName.equals(tNum)) {
                    messagingTemplate.convertAndSend("/topic/table/" + tName + "/status", event);
                }
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

    @Override
    @Transactional
    public Map<String, Object> reportOutOfStock(Long orderItemId, Long menuItemId, String reason) {
        MenuItem menuItem = null;
        if (orderItemId != null) {
            OrderItem oi = orderItemRepository.findById(orderItemId).orElse(null);
            if (oi != null && oi.getMenuItem() != null) {
                menuItem = oi.getMenuItem();
            }
        }
        if (menuItem == null && menuItemId != null) {
            menuItem = menuItemRepository.findById(menuItemId).orElse(null);
        }
        if (menuItem == null) {
            throw new ResourceNotFoundException("MenuItem", "id", menuItemId != null ? menuItemId : orderItemId);
        }

        // 1. Cập nhật MenuItem: isAvailable = false
        menuItem.setIsAvailable(false);
        menuItemRepository.save(menuItem);
        log.info("Bếp đã báo hết món: {} (ID: {})", menuItem.getName(), menuItem.getId());

        // 2. Tìm tất cả Order đang hoạt động (PENDING, COOKING)
        List<Order> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.PENDING, OrderStatus.COOKING)
        );

        Long targetMenuId = menuItem.getId();
        Set<RestaurantTable> affectedTables = new HashSet<>();
        int removedCount = 0;

        for (Order order : activeOrders) {
            // Chỉ xóa các món ĐANG HOẠT ĐỘNG (COOKING: Đang chế biến, SERVED: Chờ bưng)
            // TUYỆT ĐỐI KHÔNG XÓA món đã phục vụ lên bàn (DELIVERED) hoặc đã hủy trước đó (CANCELLED)
            List<OrderItem> toRemove = order.getOrderItems().stream()
                    .filter(item -> item.getMenuItem() != null
                            && item.getMenuItem().getId().equals(targetMenuId)
                            && (item.getStatus() == OrderItemStatus.COOKING || item.getStatus() == OrderItemStatus.SERVED))
                    .collect(Collectors.toList());

            if (!toRemove.isEmpty()) {
                if (order.getRestaurantTable() != null) {
                    affectedTables.add(order.getRestaurantTable());
                }
                for (OrderItem item : toRemove) {
                    order.removeOrderItem(item);
                    removedCount++;
                }

                if (order.getOrderItems().isEmpty()) {
                    orderRepository.delete(order);
                } else {
                    BigDecimal newTotal = order.getOrderItems().stream()
                            .map(OrderItem::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    order.setTotalAmount(newTotal);

                    // Kiểm tra xem tất cả các món còn lại có phải đã xong không
                    boolean allDone = order.getOrderItems().stream()
                            .allMatch(i -> i.getStatus() == OrderItemStatus.SERVED || i.getStatus() == OrderItemStatus.DELIVERED);
                    if (allDone && !order.getOrderItems().isEmpty()) {
                        order.setStatus(OrderStatus.COMPLETED);
                    }
                    orderRepository.save(order);
                }
            }
        }

        // 3. Chuẩn bị message cáo lỗi lịch sự
        String politeMessage = "Kính thưa Quý khách, Nhà hàng Hỏa Diệm Các thành thật cáo lỗi: Món \""
                + menuItem.getName()
                + "\" hiện tại bếp đã tạm hết nguyên liệu tươi ngon nhất. Món ăn này đã được tự động gỡ khỏi đơn gọi của bàn để Quý khách không phải chờ đợi. Kính mong Quý khách lượng thứ và hoan hỷ lựa chọn món thơm ngon khác trong thực đơn!";

        // 4. Bắn WebSocket STOMP
        try {
            // A. Thông báo cập nhật trạng thái thực đơn tới tất cả client (hiện watermark SOLD OUT)
            Map<String, Object> menuUpdateEvent = Map.of(
                    "type", "MENU_ITEM_OUT_OF_STOCK",
                    "menuItemId", menuItem.getId(),
                    "menuItemCode", menuItem.getCode(),
                    "name", menuItem.getName(),
                    "isAvailable", false,
                    "politeMessage", politeMessage
            );
            messagingTemplate.convertAndSend("/topic/menu-items", menuUpdateEvent);

            // B. Thông báo tới Bếp KDS & Phục vụ (cập nhật lại hàng đợi)
            List<OrderResponse> updatedQueue = getKitchenQueue();
            Map<String, Object> kitchenEvent = Map.of(
                    "type", "KITCHEN_ITEM_OUT_OF_STOCK",
                    "menuItemId", menuItem.getId(),
                    "menuItemName", menuItem.getName(),
                    "removedCount", removedCount,
                    "queue", updatedQueue
            );
            messagingTemplate.convertAndSend("/topic/kitchen/orders", kitchenEvent);
            messagingTemplate.convertAndSend("/topic/waiter/orders", kitchenEvent);

            // C. Thông báo trực tiếp tới các bàn bị ảnh hưởng
            for (RestaurantTable table : affectedTables) {
                Map<String, Object> tableNotice = Map.of(
                        "type", "ITEM_OUT_OF_STOCK_CANCELLED",
                        "menuItemId", menuItem.getId(),
                        "menuItemName", menuItem.getName(),
                        "message", politeMessage,
                        "tableCode", table.getTableNumber() != null ? table.getTableNumber() : "",
                        "tableName", table.getName() != null ? table.getName() : ""
                );
                if (table.getTableNumber() != null) {
                    messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/status", tableNotice);
                }
                if (table.getName() != null && !table.getName().equals(table.getTableNumber())) {
                    messagingTemplate.convertAndSend("/topic/table/" + table.getName() + "/status", tableNotice);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket thông báo hết món: {}", e.getMessage());
        }

        return Map.of(
                "success", true,
                "menuItemId", menuItem.getId(),
                "menuItemName", menuItem.getName(),
                "removedItemsCount", removedCount,
                "affectedTablesCount", affectedTables.size(),
                "message", politeMessage
        );
    }

    @Override
    @Transactional
    public Map<String, Object> restockMenuItem(Long menuItemId) {
        if (menuItemId == null) {
            throw new ResourceNotFoundException("MenuItem", "id", null);
        }

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", menuItemId));

        menuItem.setIsAvailable(true);
        menuItemRepository.save(menuItem);
        log.info("Bếp đã mở bán lại món ăn: {} (ID: {})", menuItem.getName(), menuItem.getId());

        try {
            Map<String, Object> event = Map.of(
                    "type", "MENU_ITEM_RESTOCKED",
                    "menuItemId", menuItem.getId(),
                    "menuItemCode", menuItem.getCode(),
                    "name", menuItem.getName(),
                    "isAvailable", true,
                    "message", "Món ăn '" + menuItem.getName() + "' đã mở bán trở lại"
            );
            messagingTemplate.convertAndSend("/topic/menu-items", event);
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket mở bán lại món: {}", e.getMessage());
        }

        return Map.of(
                "success", true,
                "menuItemId", menuItem.getId(),
                "name", menuItem.getName(),
                "isAvailable", true
        );
    }
}
