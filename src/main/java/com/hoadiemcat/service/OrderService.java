package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.CreateOrderRequest;
import com.hoadiemcat.dto.response.OrderItemResponse;
import com.hoadiemcat.dto.response.OrderResponse;
import com.hoadiemcat.entity.enums.OrderItemStatus;
import com.hoadiemcat.entity.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> getTableOrders(String tableNumber);

    List<OrderResponse> getKitchenQueue();

    OrderItemResponse updateOrderItemStatus(Long orderItemId, OrderItemStatus status);

    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
}
