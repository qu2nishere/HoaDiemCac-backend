package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.CreateOrderRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.OrderResponse;
import com.hoadiemcat.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Order API", description = "API đặt món từ bàn ăn khách hàng gửi vào bếp")
public class OrderCustomerController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Khách hàng gửi đợt món ăn vào bếp chế biến (UC05)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Gửi món vào bếp thành công! Bếp đang bắt đầu chế biến.", response));
    }

    @GetMapping("/{tableNumber}")
    @Operation(summary = "Lấy danh sách các đợt order của bàn hiện tại (UC06)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getTableOrders(@PathVariable String tableNumber) {
        List<OrderResponse> orders = orderService.getTableOrders(tableNumber);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
