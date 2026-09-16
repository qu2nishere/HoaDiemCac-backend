package com.hoadiemcat.controller;

import com.hoadiemcat.controller.api.MenuItemController;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MenuItemControllerTest {

    private MockMvc mockMvc;

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
}
