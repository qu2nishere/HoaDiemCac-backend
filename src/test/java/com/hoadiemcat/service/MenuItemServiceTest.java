package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.CategoryRequest;
import com.hoadiemcat.dto.request.MenuItemRequest;
import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.entity.Category;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.exception.AppException;
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
                .isSystem(false)
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
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(testCategory));

        List<CategoryResponse> categories = menuItemService.getAllCategories();

        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("nuoc-lau", categories.get(0).getSlug());
        verify(categoryRepository, times(1)).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Tạo mới danh mục thành công")
    void testCreateCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Khai Vị Mới")
                .slug("khai-vi-moi")
                .displayOrder(5)
                .isActive(true)
                .build();

        when(categoryRepository.existsBySlug("khai-vi-moi")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(2L);
            return cat;
        });

        CategoryResponse response = menuItemService.createCategory(request);

        assertNotNull(response);
        assertEquals("Khai Vị Mới", response.getName());
        assertEquals("khai-vi-moi", response.getSlug());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Cập nhật danh mục thành công")
    void testUpdateCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Nước Lẩu Đặc Biệt")
                .slug("nuoc-lau-dac-biet")
                .displayOrder(2)
                .isActive(true)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsBySlugAndIdNot("nuoc-lau-dac-biet", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = menuItemService.updateCategory(1L, request);

        assertNotNull(response);
        assertEquals("Nước Lẩu Đặc Biệt", response.getName());
        assertEquals("nuoc-lau-dac-biet", response.getSlug());
    }

    @Test
    @DisplayName("Xóa danh mục thành công")
    void testDeleteCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        doNothing().when(categoryRepository).delete(testCategory);

        assertDoesNotThrow(() -> menuItemService.deleteCategory(1L));
        verify(categoryRepository, times(1)).delete(testCategory);
    }

    @Test
    @DisplayName("Bật/Tắt trạng thái hiển thị của danh mục")
    void testToggleCategoryStatus() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = menuItemService.toggleCategoryStatus(1L);

        assertNotNull(response);
        assertFalse(response.getIsActive()); // Ban đầu isActive là true -> toggle thành false
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
        assertFalse(response.getIsAvailable());
        verify(menuItemRepository, times(1)).save(testItem);
    }

    @Test
    @DisplayName("Tạo món ăn mới thành công và lưu vào Database")
    void testCreateMenuItem_Success() {
        MenuItemRequest request = MenuItemRequest.builder()
                .code("M99")
                .name("Bò Wagyu Hỏa Diệm")
                .description("Bò thượng hạng")
                .price(new BigDecimal("450000"))
                .unit("Đĩa 200g")
                .imageUrl("https://cdn.example.com/wagyu.jpg")
                .categoryId("nuoc-lau")
                .isAvailable(true)
                .isFeatured(true)
                .build();

        when(menuItemRepository.existsByCode("M99")).thenReturn(false);
        when(categoryRepository.findBySlug("nuoc-lau")).thenReturn(Optional.of(testCategory));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem m = invocation.getArgument(0);
            m.setId(99L);
            return m;
        });

        MenuItemResponse response = menuItemService.createMenuItem(request);

        assertNotNull(response);
        assertEquals("M99", response.getCode());
        assertEquals("Bò Wagyu Hỏa Diệm", response.getName());
        assertEquals(new BigDecimal("450000"), response.getPrice());
        assertEquals("https://cdn.example.com/wagyu.jpg", response.getImage());
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Ném lỗi khi tạo món ăn với mã code đã tồn tại")
    void testCreateMenuItem_CodeExists() {
        MenuItemRequest request = MenuItemRequest.builder()
                .code("M01")
                .name("Trùng code")
                .price(new BigDecimal("100000"))
                .build();

        when(menuItemRepository.existsByCode("M01")).thenReturn(true);

        assertThrows(AppException.class, () -> menuItemService.createMenuItem(request));
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật món ăn và link CDN ảnh thành công")
    void testUpdateMenuItem_Success() {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Lẩu 9 Ngăn Trùng Khánh Siêu Cay")
                .price(new BigDecimal("420000"))
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v1/dish.jpg")
                .build();

        when(menuItemRepository.findByIdWithCategories(10L)).thenReturn(Optional.of(testItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuItemService.updateMenuItem(10L, request);

        assertNotNull(response);
        assertEquals("Lẩu 9 Ngăn Trùng Khánh Siêu Cay", response.getName());
        assertEquals(new BigDecimal("420000"), response.getPrice());
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/dish.jpg", response.getImage());
        verify(menuItemRepository, times(1)).save(testItem);
    }

    @Test
    @DisplayName("Ném lỗi ResourceNotFoundException khi cập nhật món không tồn tại")
    void testUpdateMenuItem_NotFound() {
        MenuItemRequest request = MenuItemRequest.builder().name("Test").price(BigDecimal.ONE).build();
        when(menuItemRepository.findByIdWithCategories(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.updateMenuItem(999L, request));
    }

    @Test
    @DisplayName("Xóa mềm món ăn thành công khỏi thực đơn")
    void testDeleteMenuItem_Success() {
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(testItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        menuItemService.deleteMenuItem(10L);

        assertTrue(testItem.getIsDeleted());
        verify(menuItemRepository, times(1)).save(testItem);
    }
}
