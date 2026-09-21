package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.OrderItemResponse;
import com.hoadiemcat.dto.response.OrderResponse;
import com.hoadiemcat.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/waiter")
@RequiredArgsConstructor
@Tag(name = "Waiter API", description = "API quản lý phục vụ bưng món lên bàn cho thực khách")
public class WaiterController {

    private final OrderService orderService;

    @GetMapping("/orders")
    @Operation(summary = "Lấy danh sách các đơn hàng cần theo dõi và phục vụ")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getWaiterOrders() {
        List<OrderResponse> orders = orderService.getWaiterOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/items/{orderItemId}/deliver")
    @Operation(summary = "Xác nhận đã bưng món lên bàn: SERVED -> DELIVERED")
    public ResponseEntity<ApiResponse<OrderItemResponse>> deliverOrderItem(@PathVariable Long orderItemId) {
        OrderItemResponse response = orderService.deliverOrderItem(orderItemId);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận phục vụ món lên bàn", response));
    }

    @PatchMapping("/orders/{orderId}/deliver-all")
    @Operation(summary = "Xác nhận đã bưng toàn bộ món của đợt gọi lên bàn")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverAllOrderItems(@PathVariable Long orderId) {
        OrderResponse response = orderService.deliverAllOrderItems(orderId);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn tất bưng toàn bộ món cho bàn", response));
    }
}
