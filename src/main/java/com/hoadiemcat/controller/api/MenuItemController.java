package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.CategoryRequest;
import com.hoadiemcat.dto.request.MenuItemRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu-items")
@RequiredArgsConstructor
@Tag(name = "Menu Item", description = "APIs quản lý thực đơn món ăn và danh mục nhà hàng Hỏa Diệm Các")
public class MenuItemController {

    private final MenuItemService menuItemService;

    // ==========================================
    // MÓN ĂN (MENU ITEMS)
    // ==========================================

    @GetMapping
    @Operation(summary = "Lấy toàn bộ món ăn", description = "Truy vấn danh sách tất cả các món ăn còn hoạt động từ Database kèm danh mục")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAllMenuItems() {
        List<MenuItemResponse> menuItems = menuItemService.getAllMenuItems();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách món ăn thành công", menuItems));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết món ăn", description = "Lấy thông tin chi tiết một món ăn theo ID")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItemById(@PathVariable Long id) {
        MenuItemResponse item = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết món ăn thành công", item));
    }

    @PostMapping
    @Operation(summary = "Thêm mới món ăn", description = "Tạo một món ăn mới trong thực đơn và lưu vào Database")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(@Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = menuItemService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo món ăn mới thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật món ăn", description = "Chỉnh sửa thông tin món ăn và link CDN ảnh trong Database")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request
    ) {
        MenuItemResponse updated = menuItemService.updateMenuItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật món ăn thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa món ăn", description = "Xóa mềm món ăn khỏi thực đơn nhà hàng")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Long id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa món ăn thành công", null));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Bật/Tắt trạng thái phục vụ món ăn", description = "Chuyển đổi trạng thái Còn hàng / Hết hàng của món ăn theo thời gian thực")
    public ResponseEntity<ApiResponse<MenuItemResponse>> toggleMenuItemStatus(@PathVariable Long id) {
        MenuItemResponse updated = menuItemService.toggleAvailability(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái món ăn thành công", updated));
    }

    // ==========================================
    // DANH MỤC MÓN ĂN (CATEGORIES)
    // ==========================================

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh mục món ăn", description = "Truy vấn danh sách các nhóm danh mục món ăn trong hệ thống")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = menuItemService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục món ăn thành công", categories));
    }

    @PostMapping("/categories")
    @Operation(summary = "Thêm mới danh mục", description = "Tạo một nhóm danh mục món ăn mới vào Database")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = menuItemService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục mới thành công", created));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Cập nhật danh mục", description = "Chỉnh sửa tên, slug, thứ tự hiển thị và mô tả danh mục")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse updated = menuItemService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", updated));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Xóa danh mục", description = "Xóa nhóm danh mục món ăn khỏi Database")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        menuItemService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa danh mục thành công", null));
    }

    @PatchMapping("/categories/{id}/toggle-status")
    @Operation(summary = "Bật/Tắt trạng thái hiển thị danh mục", description = "Ẩn hoặc hiện danh mục trên thực đơn khách hàng")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleCategoryStatus(@PathVariable Long id) {
        CategoryResponse updated = menuItemService.toggleCategoryStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái danh mục thành công", updated));
    }
}
