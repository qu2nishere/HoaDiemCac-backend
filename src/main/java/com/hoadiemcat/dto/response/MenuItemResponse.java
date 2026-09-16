package com.hoadiemcat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private String unit;
    private String imageUrl;
    private String image;
    private Boolean isAvailable;
    private Boolean isFeatured;
    private Long totalOrderedCount;
    private String categoryId;
    private String categoryName;
    private List<CategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
