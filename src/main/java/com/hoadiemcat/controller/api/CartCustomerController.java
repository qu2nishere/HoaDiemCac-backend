package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.DraftCartItemResponse;
import com.hoadiemcat.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/cart")
@RequiredArgsConstructor
@Tag(name = "Customer Cart API", description = "API quản lý giỏ hàng cộng tác thời gian thực tại bàn (UC04 - Realtime Cart)")
public class CartCustomerController {

    private final CartService cartService;

    @GetMapping("/{tableNumber}")
    @Operation(summary = "Lấy giỏ hàng nháp hiện tại của bàn ăn")
    public ResponseEntity<ApiResponse<List<DraftCartItemResponse>>> getCart(@PathVariable String tableNumber) {
        List<DraftCartItemResponse> cart = cartService.getCart(tableNumber);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PutMapping("/{tableNumber}")
    @Operation(summary = "Cập nhật giỏ hàng chung của bàn và đồng bộ qua WebSocket")
    public ResponseEntity<ApiResponse<List<DraftCartItemResponse>>> updateCart(
            @PathVariable String tableNumber,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody List<DraftCartItemRequest> items
    ) {
        List<DraftCartItemResponse> updated = cartService.updateCart(tableNumber, items, deviceToken);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ giỏ hàng thành công!", updated));
    }

    @PostMapping("/{tableNumber}")
    @Operation(summary = "Cập nhật giỏ hàng chung của bàn (hỗ trợ POST method)")
    public ResponseEntity<ApiResponse<List<DraftCartItemResponse>>> updateCartPost(
            @PathVariable String tableNumber,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody List<DraftCartItemRequest> items
    ) {
        List<DraftCartItemResponse> updated = cartService.updateCart(tableNumber, items, deviceToken);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ giỏ hàng thành công!", updated));
    }

    @DeleteMapping("/{tableNumber}")
    @Operation(summary = "Xóa toàn bộ giỏ hàng nháp của bàn")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String tableNumber) {
        cartService.clearCart(tableNumber);
        return ResponseEntity.ok(ApiResponse.success("Đã làm rỗng giỏ hàng của bàn!", null));
    }

    @MessageMapping("/table/{tableNumber}/cart")
    public void handleSocketCartMessage(@DestinationVariable String tableNumber, List<DraftCartItemRequest> items) {
        cartService.updateCart(tableNumber, items);
    }
}
