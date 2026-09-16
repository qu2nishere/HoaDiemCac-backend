package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.CategoryRequest;
import com.hoadiemcat.dto.request.MenuItemRequest;
import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;

import java.util.List;

public interface MenuItemService {

    List<MenuItemResponse> getAllMenuItems();

    List<CategoryResponse> getAllCategories();

    MenuItemResponse getMenuItemById(Long id);

    MenuItemResponse toggleAvailability(Long id);

    MenuItemResponse createMenuItem(MenuItemRequest request);

    MenuItemResponse updateMenuItem(Long id, MenuItemRequest request);

    void deleteMenuItem(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    CategoryResponse toggleCategoryStatus(Long id);
}
