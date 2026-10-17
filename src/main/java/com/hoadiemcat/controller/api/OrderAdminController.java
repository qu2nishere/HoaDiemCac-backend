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

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Order Adjustment API", description = "Quản lý và điều chỉnh order của bàn khi món đã gửi bếp (UC13)")
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping("/tables/{tableIdentifier}")
    @Operation(summary = "Lấy toàn bộ các đợt order của bàn dành cho Quản lý (UC13)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getTableOrders(@PathVariable String tableIdentifier) {
        List<OrderResponse> orders = orderService.getTableOrders(tableIdentifier);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/items/{orderItemId}/quantity")
    @Operation(summary = "Quản lý điều chỉnh số lượng món ăn đã gửi bếp (UC13)")
    public ResponseEntity<ApiResponse<OrderItemResponse>> updateQuantity(
            @PathVariable Long orderItemId,
            @RequestBody Map<String, Object> body,
            Principal principal
    ) {
        int quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 1;
        String actor = principal != null ? principal.getName() : "ADMIN";
        OrderItemResponse res = orderService.updateOrderItemQuantity(orderItemId, quantity, actor);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật số lượng món thành công", res));
    }

    @PatchMapping("/items/{orderItemId}/note")
    @Operation(summary = "Quản lý điều chỉnh ghi chú món ăn đã gửi bếp (UC13)")
    public ResponseEntity<ApiResponse<OrderItemResponse>> updateNote(
            @PathVariable Long orderItemId,
            @RequestBody Map<String, Object> body,
            Principal principal
    ) {
        String note = body.get("note") != null ? body.get("note").toString() : "";
        String actor = principal != null ? principal.getName() : "ADMIN";
        OrderItemResponse res = orderService.updateOrderItemNote(orderItemId, note, actor);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật ghi chú món thành công", res));
    }

    @PostMapping("/items/{orderItemId}/cancel")
    @Operation(summary = "Quản lý hủy/xóa món ăn khỏi order kèm lý do bắt buộc (UC13, BR13.1)")
    public ResponseEntity<ApiResponse<OrderItemResponse>> cancelOrderItem(
            @PathVariable Long orderItemId,
            @RequestBody Map<String, Object> body,
            Principal principal
    ) {
        String reason = body.get("reason") != null ? body.get("reason").toString() : "Khách yêu cầu hủy món";
        String actor = principal != null ? principal.getName() : "ADMIN";
        OrderItemResponse res = orderService.cancelOrderItem(orderItemId, reason, actor);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy món thành công khỏi order của bàn", res));
    }
}
