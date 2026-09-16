package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.CategoryRequest;
import com.hoadiemcat.dto.request.MenuItemRequest;
import com.hoadiemcat.dto.response.CategoryResponse;
import com.hoadiemcat.dto.response.MenuItemResponse;
import com.hoadiemcat.entity.Category;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.CategoryRepository;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
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
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
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

    @Override
    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = "M" + (System.currentTimeMillis() % 1000000);
        } else {
            code = code.trim().toUpperCase();
            if (menuItemRepository.existsByCode(code)) {
                throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã món ăn '" + code + "' đã tồn tại");
            }
        }

        Set<Category> categories = resolveCategories(request);

        MenuItem item = MenuItem.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .unit(request.getUnit() != null && !request.getUnit().isBlank() ? request.getUnit().trim() : "Phần")
                .imageUrl(request.getEffectiveImageUrl())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isDeleted(false)
                .totalOrderedCount(0L)
                .categories(categories)
                .build();

        MenuItem saved = menuItemRepository.save(item);
        log.info("Đã tạo mới món ăn ID {}: {} (Mã: {}, URL: {})", saved.getId(), saved.getName(), saved.getCode(), saved.getImageUrl());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String newCode = request.getCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(item.getCode()) && menuItemRepository.existsByCode(newCode)) {
                throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã món ăn '" + newCode + "' đã thuộc về món khác");
            }
            item.setCode(newCode);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }

        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }

        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            item.setUnit(request.getUnit().trim());
        }

        String effectiveImageUrl = request.getEffectiveImageUrl();
        if (effectiveImageUrl != null) {
            item.setImageUrl(effectiveImageUrl);
        }

        if (request.getIsAvailable() != null) {
            item.setIsAvailable(request.getIsAvailable());
        }

        if (request.getIsFeatured() != null) {
            item.setIsFeatured(request.getIsFeatured());
        }

        if ((request.getCategoryIds() != null && !request.getCategoryIds().isEmpty())
                || (request.getCategoryId() != null && !request.getCategoryId().isBlank())) {
            Set<Category> updatedCategories = resolveCategories(request);
            if (!updatedCategories.isEmpty()) {
                item.setCategories(updatedCategories);
            }
        }

        MenuItem saved = menuItemRepository.save(item);
        log.info("Đã cập nhật món ăn ID {}: {} (Link CDN: {})", saved.getId(), saved.getName(), saved.getImageUrl());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));

        item.setIsDeleted(true);
        menuItemRepository.save(item);
        log.info("Đã xóa mềm món ăn ID {}: {}", id, item.getName());
    }

    private Set<Category> resolveCategories(MenuItemRequest request) {
        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> found = categoryRepository.findAllById(request.getCategoryIds());
            categories.addAll(found);
        } else if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            String catIdStr = request.getCategoryId().trim();
            if (catIdStr.matches("\\d+")) {
                categoryRepository.findById(Long.parseLong(catIdStr)).ifPresent(categories::add);
            }
            if (categories.isEmpty()) {
                categoryRepository.findBySlug(catIdStr).ifPresent(categories::add);
            }
        }
        return categories;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? sanitizeSlug(request.getSlug())
                : generateSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã slug danh mục '" + slug + "' đã tồn tại");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isSystem(false)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Đã tạo mới danh mục: {} (slug: {})", saved.getName(), saved.getSlug());
        return mapCategoryToResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? sanitizeSlug(request.getSlug())
                : generateSlug(request.getName());

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã slug danh mục '" + slug + "' đã thuộc về danh mục khác");
        }

        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        Category updated = categoryRepository.save(category);
        log.info("Đã cập nhật danh mục ID {}: {}", id, updated.getName());
        return mapCategoryToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể xóa danh mục mặc định của hệ thống");
        }

        // Gỡ liên kết trong bảng menu_item_categories
        if (category.getMenuItems() != null) {
            for (MenuItem item : category.getMenuItems()) {
                item.getCategories().remove(category);
            }
        }

        categoryRepository.delete(category);
        log.info("Đã xóa danh mục ID: {}", id);
    }

    @Override
    @Transactional
    public CategoryResponse toggleCategoryStatus(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        boolean nextStatus = !Boolean.TRUE.equals(category.getIsActive());
        category.setIsActive(nextStatus);
        Category saved = categoryRepository.save(category);

        log.info("Cập nhật trạng thái danh mục ID {}: {}", id, nextStatus ? "Hiển thị" : "Tạm ẩn");
        return mapCategoryToResponse(saved);
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
                .image(item.getImageUrl())
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
        long itemCount = 0L;
        if (category.getMenuItems() != null) {
            itemCount = category.getMenuItems().stream()
                    .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                    .count();
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .isSystem(category.getIsSystem())
                .totalItems(itemCount)
                .build();
    }

    private String generateSlug(String text) {
        if (text == null || text.isBlank()) {
            return "cat-" + System.currentTimeMillis();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.toLowerCase()
                .replaceAll("[đĐ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return slug.isEmpty() ? "cat-" + System.currentTimeMillis() : slug;
    }

    private String sanitizeSlug(String slug) {
        return slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
    }
}
