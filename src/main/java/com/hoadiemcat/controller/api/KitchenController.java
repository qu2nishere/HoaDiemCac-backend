package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.OrderItemResponse;
import com.hoadiemcat.dto.response.OrderResponse;
import com.hoadiemcat.entity.enums.OrderItemStatus;
import com.hoadiemcat.entity.enums.OrderStatus;
import com.hoadiemcat.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kitchen")
@RequiredArgsConstructor
@Tag(name = "Kitchen KDS API", description = "API quản lý hàng đợi và cập nhật tiến độ chế biến trạm Bếp KDS")
public class KitchenController {

    private final OrderService orderService;

    @GetMapping("/queue")
    @Operation(summary = "Lấy toàn bộ hàng đợi món cần chế biến theo chuẩn FIFO (UC17)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getKitchenQueue() {
        List<OrderResponse> queue = orderService.getKitchenQueue();
        return ResponseEntity.ok(ApiResponse.success(queue));
    }

    @PatchMapping("/items/{orderItemId}/status")
    @Operation(summary = "Cập nhật trạng thái chế biến món ăn: COOKING -> SERVED (UC18)")
    public ResponseEntity<ApiResponse<OrderItemResponse>> updateOrderItemStatus(
            @PathVariable Long orderItemId,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        OrderItemStatus status = OrderItemStatus.valueOf(statusStr.toUpperCase());
        OrderItemResponse response = orderService.updateOrderItemStatus(orderItemId, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái món thành công", response));
    }

    @PatchMapping("/orders/{orderId}/status")
    @Operation(summary = "Cập nhật trạng thái toàn bộ đợt order của bàn")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        OrderStatus status = OrderStatus.valueOf(statusStr.toUpperCase());
        OrderResponse response = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đợt gọi thành công", response));
    }
}
