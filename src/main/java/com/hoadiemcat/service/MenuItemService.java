package com.hoadiemcat.service;

import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;

import java.util.List;

public interface MenuItemService {

    List<MenuItemResponse> getAllMenuItems();

    List<CategoryResponse> getAllCategories();

    MenuItemResponse getMenuItemById(Long id);

    MenuItemResponse toggleAvailability(Long id);
}
