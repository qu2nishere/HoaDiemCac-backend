package com.hoadiemcat.service;

import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.entity.Category;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.CategoryRepository;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.service.impl.MenuItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    private Category testCategory;
    private MenuItem testItem;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .name("Nước Lẩu Hoàng Gia")
                .slug("nuoc-lau")
                .displayOrder(1)
                .isActive(true)
                .build();
        testCategory.setId(1L);

        testItem = MenuItem.builder()
                .code("M01")
                .name("Lẩu 9 Ngăn Trùng Khánh")
                .description("Cay nồng Tứ Xuyên")
                .price(new BigDecimal("389000"))
                .unit("Nồi 9 ngăn")
                .imageUrl("https://example.com/lau.jpg")
                .isAvailable(true)
                .isFeatured(true)
                .isDeleted(false)
                .totalOrderedCount(50L)
                .categories(new HashSet<>(Set.of(testCategory)))
                .build();
        testItem.setId(10L);
    }

    @Test
    @DisplayName("Lấy toàn bộ món ăn thành công từ Database")
    void testGetAllMenuItems() {
        when(menuItemRepository.findAllActiveWithCategories()).thenReturn(List.of(testItem));

        List<MenuItemResponse> responses = menuItemService.getAllMenuItems();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        MenuItemResponse itemRes = responses.get(0);
        assertEquals("M01", itemRes.getCode());
        assertEquals("Lẩu 9 Ngăn Trùng Khánh", itemRes.getName());
        assertEquals("nuoc-lau", itemRes.getCategoryId());
        assertEquals("Nước Lẩu Hoàng Gia", itemRes.getCategoryName());
        assertEquals("https://example.com/lau.jpg", itemRes.getImage());
        assertTrue(itemRes.getIsAvailable());
        verify(menuItemRepository, times(1)).findAllActiveWithCategories();
    }

    @Test
    @DisplayName("Lấy danh mục món ăn thành công")
    void testGetAllCategories() {
        when(categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(testCategory));

        List<CategoryResponse> categories = menuItemService.getAllCategories();

        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("nuoc-lau", categories.get(0).getSlug());
        verify(categoryRepository, times(1)).findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Lấy món ăn theo ID thành công")
    void testGetMenuItemById_Success() {
        when(menuItemRepository.findByIdWithCategories(10L)).thenReturn(Optional.of(testItem));

        MenuItemResponse response = menuItemService.getMenuItemById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Lẩu 9 Ngăn Trùng Khánh", response.getName());
    }

    @Test
    @DisplayName("Ném lỗi ResourceNotFoundException khi không tìm thấy món theo ID")
    void testGetMenuItemById_NotFound() {
        when(menuItemRepository.findByIdWithCategories(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.getMenuItemById(999L));
    }

    @Test
    @DisplayName("Bật/Tắt trạng thái phục vụ của món ăn thành công")
    void testToggleAvailability() {
        when(menuItemRepository.findByIdWithCategories(10L)).thenReturn(Optional.of(testItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuItemService.toggleAvailability(10L);

        assertNotNull(response);
        assertFalse(response.getIsAvailable()); // Trước đó là true -> toggle thành false
        verify(menuItemRepository, times(1)).save(testItem);
    }
}
