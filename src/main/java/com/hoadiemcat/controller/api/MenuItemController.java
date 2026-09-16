package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu-items")
@RequiredArgsConstructor
@Tag(name = "Menu Item", description = "APIs quản lý thực đơn món ăn và danh mục nhà hàng Hỏa Diệm Các")
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping
    @Operation(summary = "Lấy toàn bộ món ăn", description = "Truy vấn danh sách tất cả các món ăn còn hoạt động từ Database kèm danh mục")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAllMenuItems() {
        List<MenuItemResponse> menuItems = menuItemService.getAllMenuItems();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách món ăn thành công", menuItems));
    }

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh mục món ăn", description = "Truy vấn danh sách các nhóm danh mục món ăn đang kích hoạt")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = menuItemService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục món ăn thành công", categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết món ăn", description = "Lấy thông tin chi tiết một món ăn theo ID")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItemById(@PathVariable Long id) {
        MenuItemResponse item = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết món ăn thành công", item));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Bật/Tắt trạng thái phục vụ món ăn", description = "Chuyển đổi trạng thái Còn hàng / Hết hàng của món ăn theo thời gian thực")
    public ResponseEntity<ApiResponse<MenuItemResponse>> toggleMenuItemStatus(@PathVariable Long id) {
        MenuItemResponse updated = menuItemService.toggleAvailability(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái món ăn thành công", updated));
    }
}
