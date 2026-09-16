package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.entity.Category;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.CategoryRepository;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        List<MenuItem> items = menuItemRepository.findAllActiveWithCategories();
        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapCategoryToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public MenuItemResponse toggleAvailability(Long id) {
        MenuItem item = menuItemRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));

        boolean currentStatus = Boolean.TRUE.equals(item.getIsAvailable());
        item.setIsAvailable(!currentStatus);
        MenuItem saved = menuItemRepository.save(item);

        log.info("Cập nhật trạng thái món ăn ID {}: {}", id, saved.getIsAvailable() ? "Còn hàng" : "Hết hàng");
        return mapToResponse(saved);
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        Set<Category> categories = item.getCategories();
        List<CategoryResponse> categoryResponses = categories != null
                ? categories.stream()
                .sorted(Comparator.comparing(Category::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapCategoryToResponse)
                .collect(Collectors.toList())
                : List.of();

        String primaryCategoryId = "khac";
        String primaryCategoryName = "Món Khác";

        if (!categoryResponses.isEmpty()) {
            CategoryResponse first = categoryResponses.get(0);
            primaryCategoryId = first.getSlug();
            primaryCategoryName = first.getName();
        }

        return MenuItemResponse.builder()
                .id(item.getId())
                .code(item.getCode())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .unit(item.getUnit())
                .imageUrl(item.getImageUrl())
                .image(item.getImageUrl()) // Alias cho tương thích giao diện Client
                .isAvailable(item.getIsAvailable())
                .isFeatured(item.getIsFeatured())
                .totalOrderedCount(item.getTotalOrderedCount())
                .categoryId(primaryCategoryId)
                .categoryName(primaryCategoryName)
                .categories(categoryResponses)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private CategoryResponse mapCategoryToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .build();
    }
}
