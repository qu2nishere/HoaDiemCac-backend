package com.hoadiemcat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoadiemcat.controller.api.MenuItemController;
import com.hoadiemcat.dto.request.CategoryRequest;
import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.service.MenuItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MenuItemControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MenuItemService menuItemService;

    @InjectMocks
    private MenuItemController menuItemController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(menuItemController).build();
    }

    @Test
    @DisplayName("API GET /api/v1/menu-items trả về danh sách món ăn")
    void testGetAllMenuItems() throws Exception {
        MenuItemResponse item = MenuItemResponse.builder()
                .id(1L)
                .code("M01")
                .name("Lẩu 9 Ngăn Trùng Khánh")
                .price(new BigDecimal("389000"))
                .isAvailable(true)
                .categoryId("nuoc-lau")
                .categoryName("Nước Lẩu Hoàng Gia")
                .build();

        when(menuItemService.getAllMenuItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].code").value("M01"))
                .andExpect(jsonPath("$.result[0].name").value("Lẩu 9 Ngăn Trùng Khánh"))
                .andExpect(jsonPath("$.result[0].isAvailable").value(true));
    }

    @Test
    @DisplayName("API GET /api/v1/menu-items/categories trả về danh sách danh mục")
    void testGetAllCategories() throws Exception {
        CategoryResponse category = CategoryResponse.builder()
                .id(1L)
                .name("Nước Lẩu Hoàng Gia")
                .slug("nuoc-lau")
                .displayOrder(1)
                .isActive(true)
                .build();

        when(menuItemService.getAllCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/menu-items/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].slug").value("nuoc-lau"));
    }

    @Test
    @DisplayName("API POST /api/v1/menu-items/categories tạo danh mục mới")
    void testCreateCategory() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Tráng Miệng Hoàng Gia")
                .slug("trang-mieng")
                .displayOrder(10)
                .isActive(true)
                .build();

        CategoryResponse created = CategoryResponse.builder()
                .id(3L)
                .name("Tráng Miệng Hoàng Gia")
                .slug("trang-mieng")
                .displayOrder(10)
                .isActive(true)
                .build();

        when(menuItemService.createCategory(any(CategoryRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/menu-items/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(3L))
                .andExpect(jsonPath("$.result.name").value("Tráng Miệng Hoàng Gia"));
    }

    @Test
    @DisplayName("API DELETE /api/v1/menu-items/categories/{id} xóa danh mục")
    void testDeleteCategory() throws Exception {
        doNothing().when(menuItemService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/menu-items/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    @DisplayName("API PATCH /api/v1/menu-items/{id}/toggle-status thay đổi trạng thái món")
    void testToggleMenuItemStatus() throws Exception {
        MenuItemResponse updated = MenuItemResponse.builder()
                .id(1L)
                .code("M01")
                .name("Lẩu 9 Ngăn Trùng Khánh")
                .isAvailable(false)
                .build();

        when(menuItemService.toggleAvailability(1L)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/menu-items/1/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.isAvailable").value(false));
    }

    @Test
    @DisplayName("API POST /api/v1/menu-items tạo món ăn mới thành công")
    void testCreateMenuItem() throws Exception {
        com.hoadiemcat.dto.request.MenuItemRequest request = com.hoadiemcat.dto.request.MenuItemRequest.builder()
                .name("Bò Wagyu A5")
                .price(new BigDecimal("450000"))
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v1/wagyu.jpg")
                .categoryId("thit-bo")
                .build();

        MenuItemResponse response = MenuItemResponse.builder()
                .id(20L)
                .code("M20")
                .name("Bò Wagyu A5")
                .price(new BigDecimal("450000"))
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v1/wagyu.jpg")
                .build();

        when(menuItemService.createMenuItem(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(20L))
                .andExpect(jsonPath("$.result.name").value("Bò Wagyu A5"));
    }

    @Test
    @DisplayName("API PUT /api/v1/menu-items/{id} cập nhật món ăn và link ảnh CDN")
    void testUpdateMenuItem() throws Exception {
        com.hoadiemcat.dto.request.MenuItemRequest request = com.hoadiemcat.dto.request.MenuItemRequest.builder()
                .name("Bò Wagyu A5 Đặc Biệt")
                .price(new BigDecimal("480000"))
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v2/wagyu-updated.jpg")
                .build();

        MenuItemResponse response = MenuItemResponse.builder()
                .id(20L)
                .name("Bò Wagyu A5 Đặc Biệt")
                .price(new BigDecimal("480000"))
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v2/wagyu-updated.jpg")
                .build();

        when(menuItemService.updateMenuItem(eq(20L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/menu-items/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.name").value("Bò Wagyu A5 Đặc Biệt"))
                .andExpect(jsonPath("$.result.imageUrl").value("https://res.cloudinary.com/demo/image/upload/v2/wagyu-updated.jpg"));
    }

    @Test
    @DisplayName("API DELETE /api/v1/menu-items/{id} xóa món ăn")
    void testDeleteMenuItem() throws Exception {
        doNothing().when(menuItemService).deleteMenuItem(20L);

        mockMvc.perform(delete("/api/v1/menu-items/20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }
}
