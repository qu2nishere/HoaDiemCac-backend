package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.CreateOrderRequest;
import com.hoadiemcat.dto.request.OrderItemRequest;
import com.hoadiemcat.dto.response.OrderItemResponse;
import com.hoadiemcat.dto.response.OrderResponse;
import com.hoadiemcat.entity.Cart;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.entity.Order;
import com.hoadiemcat.entity.OrderItem;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.OrderItemStatus;
import com.hoadiemcat.entity.enums.OrderStatus;
import com.hoadiemcat.entity.enums.TableStatus;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.CartRepository;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.repository.OrderItemRepository;
import com.hoadiemcat.repository.OrderRepository;
import com.hoadiemcat.repository.RestaurantTableRepository;
import com.hoadiemcat.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hoadiemcat.dto.response.TableQrResponse;
import com.hoadiemcat.entity.AuditLog;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.repository.AuditLogRepository;
import com.hoadiemcat.repository.UserRepository;
import com.hoadiemcat.service.TableQrService;
import org.springframework.context.annotation.Lazy;
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
    private final CartRepository cartRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    @Lazy private final TableQrService tableQrService;

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

        // Chuyển trạng thái bàn sang OCCUPIED và gắn sessionToken nếu bàn chưa phục vụ
        if (table.getStatus() != TableStatus.OCCUPIED || table.getCurrentSessionToken() == null) {
            table.setStatus(TableStatus.OCCUPIED);
            table.setCurrentSessionToken(sessionToken);
            if (table.getSessionStartedAt() == null) {
                table.setSessionStartedAt(java.time.LocalDateTime.now());
            }
            restaurantTableRepository.save(table);
        }

        // Tính round number chuẩn theo session hiện tại của bàn
        int roundNumber = 1;
        if (sessionToken != null && !sessionToken.isBlank()) {
            List<Order> sessionOrders = orderRepository.findByTableNumberAndSessionTokenOrderByCreatedAtAsc(table.getTableNumber(), sessionToken);
            roundNumber = sessionOrders.size() + 1;
        } else {
            List<Order> existingOrders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(table);
            roundNumber = existingOrders.size() + 1;
        }

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
            int qty = itemReq.getQuantity() != null ? Math.min(Math.max(itemReq.getQuantity(), 1), 99) : 1;
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

        // Làm rỗng giỏ hàng nháp chung của bàn sau khi đã gửi bếp thành công (UC05)
        try {
            if (cartRepository != null) {
                List<Cart> carts = cartRepository.findAllByRestaurantTable(table);
                if (!carts.isEmpty()) {
                    cartRepository.deleteAll(carts);
                }
            }
            java.util.List<?> emptyCart = java.util.Collections.emptyList();
            if (table.getTableNumber() != null) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/cart", emptyCart);
            }
            if (table.getName() != null && !table.getName().equals(table.getTableNumber())) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getName() + "/cart", emptyCart);
            }
            if (table.getCurrentSessionToken() != null) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getCurrentSessionToken() + "/cart", emptyCart);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi làm rỗng giỏ hàng sau khi gửi order: {}", e.getMessage());
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

        List<OrderResponse> result;
        if (table != null && table.getCurrentSessionToken() != null) {
            List<OrderResponse> sessionOrders = orderRepository.findByTableNumberAndSessionTokenOrderByCreatedAtAsc(normalized, table.getCurrentSessionToken())
                    .stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            if (!sessionOrders.isEmpty()) {
                result = sessionOrders;
            } else {
                result = orderRepository.findByTableNumberOrderByCreatedAtAsc(normalized)
                        .stream()
                        .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
        } else {
            result = orderRepository.findByTableNumberOrderByCreatedAtAsc(normalized)
                    .stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // Chuẩn hóa số đợt gọi món (roundNumber) tăng dần liên tục theo thứ tự thời gian gọi: 1, 2, 3...
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRoundNumber(i + 1);
        }

        return result;
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
            if (item.getServedAt() == null) {
                item.setServedAt(LocalDateTime.now());
            }
            item.setDeliveredAt(null);
        } else if (status == OrderItemStatus.DELIVERED) {
            item.setDeliveredAt(LocalDateTime.now());
            if (item.getServedAt() == null) {
                item.setServedAt(LocalDateTime.now());
            }
        } else if (status == OrderItemStatus.COOKING) {
            item.setServedAt(null);
            item.setDeliveredAt(null);
        }

        OrderItem savedItem = orderItemRepository.save(item);
        Order order = item.getOrder();

        boolean allDelivered = order.getOrderItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.DELIVERED || i.getStatus() == OrderItemStatus.CANCELLED);
        if (allDelivered) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            order.setStatus(OrderStatus.COOKING);
            orderRepository.save(order);
        }

        OrderItemResponse response = mapItemToResponse(savedItem);

        // Broadcast qua WebSocket tới KDS, Waiter và Table
        try {
            String eventType;
            if (status == OrderItemStatus.DELIVERED) {
                eventType = "WAITER_ITEM_DELIVERED";
            } else if (status == OrderItemStatus.SERVED && item.getDeliveredAt() == null) {
                eventType = "WAITER_ITEM_UNDELIVERED";
            } else {
                eventType = "KITCHEN_ITEM_STATUS_TOGGLED";
            }

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
                .cancelledAt(item.getCancelledAt())
                .cancelReason(item.getCancelReason())
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

    @Override
    @Transactional
    public OrderItemResponse updateOrderItemQuantity(Long orderItemId, int quantity, String actorUsername) {
        if (quantity < 1 || quantity > 99) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số lượng món phải từ 1 đến 99!");
        }
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));

        if (item.getStatus() == OrderItemStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Món ăn đã bị hủy trước đó, không thể chỉnh sửa số lượng!");
        }

        int oldQuantity = item.getQuantity() != null ? item.getQuantity() : 1;
        item.setQuantity(quantity);
        item.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        orderItemRepository.save(item);

        Order order = item.getOrder();
        recalculateOrderTotal(order);

        // Ghi AuditLog
        saveAdjustmentAuditLog(actorUsername, "UPDATE_QUANTITY", orderItemId,
                String.format("Điều chỉnh số lượng món '%s' từ %d thành %d",
                        item.getMenuItem() != null ? item.getMenuItem().getName() : "Món", oldQuantity, quantity));

        // Phát WebSocket đồng bộ
        broadcastOrderAdjustmentEvent("ORDER_ITEM_UPDATED", item, order, "Đã cập nhật số lượng món");

        return mapItemToResponse(item);
    }

    @Override
    @Transactional
    public OrderItemResponse updateOrderItemNote(Long orderItemId, String note, String actorUsername) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));

        if (item.getStatus() == OrderItemStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Món ăn đã bị hủy, không thể sửa ghi chú!");
        }

        String oldNote = item.getNote();
        item.setNote(note != null ? note.trim() : "");
        orderItemRepository.save(item);

        Order order = item.getOrder();

        // Ghi AuditLog
        saveAdjustmentAuditLog(actorUsername, "UPDATE_NOTE", orderItemId,
                String.format("Cập nhật ghi chú món '%s': '%s' -> '%s'",
                        item.getMenuItem() != null ? item.getMenuItem().getName() : "Món",
                        oldNote != null ? oldNote : "", item.getNote()));

        // Phát WebSocket đồng bộ
        broadcastOrderAdjustmentEvent("ORDER_ITEM_UPDATED", item, order, "Đã cập nhật ghi chú món");

        return mapItemToResponse(item);
    }

    @Override
    @Transactional
    public OrderItemResponse cancelOrderItem(Long orderItemId, String reason, String actorUsername) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));

        if (item.getStatus() == OrderItemStatus.CANCELLED) {
            return mapItemToResponse(item);
        }

        String cancelReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Khách yêu cầu hủy món";
        item.setStatus(OrderItemStatus.CANCELLED);
        item.setCancelledAt(LocalDateTime.now());
        item.setCancelReason(cancelReason);
        orderItemRepository.save(item);

        Order order = item.getOrder();
        recalculateOrderTotal(order);

        // Ghi AuditLog
        saveAdjustmentAuditLog(actorUsername, "CANCEL_ORDER_ITEM", orderItemId,
                String.format("Hủy món '%s' (SL: %d, Giá: %s). Lý do: %s",
                        item.getMenuItem() != null ? item.getMenuItem().getName() : "Món",
                        item.getQuantity(), item.getTotalPrice(), cancelReason));

        // Phát WebSocket đồng bộ
        broadcastOrderAdjustmentEvent("ORDER_ITEM_CANCELLED", item, order, "Món đã bị hủy: " + cancelReason);

        return mapItemToResponse(item);
    }

    private void recalculateOrderTotal(Order order) {
        if (order == null) return;
        List<OrderItem> items = order.getOrderItems();
        if (items != null) {
            BigDecimal total = items.stream()
                    .filter(i -> i.getStatus() != OrderItemStatus.CANCELLED)
                    .map(i -> i.getTotalPrice() != null ? i.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(total);

            boolean allCancelled = items.stream().allMatch(i -> i.getStatus() == OrderItemStatus.CANCELLED);
            if (allCancelled && !items.isEmpty()) {
                order.setStatus(OrderStatus.CANCELLED);
            } else if (order.getStatus() == OrderStatus.CANCELLED && !allCancelled) {
                order.setStatus(OrderStatus.COOKING);
            }
            orderRepository.save(order);
        }
    }

    private void saveAdjustmentAuditLog(String actorUsername, String action, Long targetId, String reason) {
        try {
            com.hoadiemcat.entity.User actor = null;
            if (actorUsername != null && !actorUsername.isBlank()) {
                actor = userRepository.findByUsername(actorUsername)
                        .or(() -> userRepository.findByEmail(actorUsername))
                        .orElse(null);
            }
            if (actor == null) {
                actor = userRepository.findAll().stream().findFirst().orElse(null);
            }

            AuditLog logEntity = AuditLog.builder()
                    .actor(actor)
                    .action(action)
                    .targetEntity("OrderItem")
                    .targetId(targetId)
                    .reason(reason)
                    .build();
            auditLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Không thể lưu AuditLog cho thao tác {}: {}", action, e.getMessage());
        }
    }

    private void broadcastOrderAdjustmentEvent(String eventType, OrderItem item, Order order, String message) {
        try {
            RestaurantTable table = order != null ? order.getRestaurantTable() : null;
            String tableNumber = table != null ? table.getTableNumber() : "";
            String tableName = table != null ? table.getName() : "";

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", eventType);
            payload.put("orderItemId", item.getId());
            payload.put("orderId", order != null ? order.getId() : 0);
            payload.put("tableNumber", tableNumber);
            payload.put("tableName", tableName);
            payload.put("itemName", item.getMenuItem() != null ? item.getMenuItem().getName() : "");
            payload.put("newQuantity", item.getQuantity() != null ? item.getQuantity() : 0);
            payload.put("newStatus", item.getStatus() != null ? item.getStatus().name() : "");
            payload.put("note", item.getNote() != null ? item.getNote() : "");
            payload.put("cancelReason", item.getCancelReason() != null ? item.getCancelReason() : "");
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());

            // 1. Gửi tới kênh khách hàng của bàn (đồng bộ giỏ hàng realtime)
            if (!tableNumber.isBlank()) {
                messagingTemplate.convertAndSend("/topic/table/" + tableNumber + "/status", payload);
            }
            if (!tableName.isBlank() && !tableName.equalsIgnoreCase(tableNumber)) {
                messagingTemplate.convertAndSend("/topic/table/" + tableName + "/status", payload);
            }

            // 2. Gửi tới Bếp KDS (thẻ món cập nhật hoặc biến mất ngay)
            messagingTemplate.convertAndSend("/topic/kitchen/orders", payload);

            // 3. Gửi tới Phục vụ
            messagingTemplate.convertAndSend("/topic/waiter/orders", payload);

            // 4. Cập nhật thẻ bàn trên sơ đồ quản lý (Admin tables)
            if (table != null && table.getId() != null) {
                try {
                    TableQrResponse tableResp = tableQrService.getTableById(table.getId());
                    messagingTemplate.convertAndSend("/topic/tables", tableResp);
                    if (!tableNumber.isBlank()) {
                        messagingTemplate.convertAndSend("/topic/table/" + tableNumber + "/status", tableResp);
                    }
                } catch (Exception tEx) {
                    log.warn("Không thể gửi cập nhật TableQrResponse qua WebSocket: {}", tEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket điều chỉnh order: {}", e.getMessage());
        }
    }
}
