package com.hoadiemcat.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequest {

    @Size(max = 30, message = "Mã món ăn tối đa 30 ký tự")
    private String code;

    @NotBlank(message = "Tên món ăn không được để trống")
    @Size(max = 200, message = "Tên món ăn tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá món ăn không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá món ăn phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Size(max = 50, message = "Đơn vị tính tối đa 50 ký tự")
    private String unit;

    @Size(max = 1000, message = "Đường dẫn ảnh tối đa 1000 ký tự")
    private String imageUrl;

    // Alias để hỗ trợ cả trường 'image' gửi từ frontend
    private String image;

    // Mã định danh danh mục (slug như 'nuoc-lau' hoặc id số '1')
    private String categoryId;

    // Hoặc danh sách ID danh mục
    private List<Long> categoryIds;

    @Builder.Default
    private Boolean isAvailable = true;

    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Lấy đường dẫn ảnh hợp lệ (ưu tiên imageUrl, fallback image)
     */
    public String getEffectiveImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.trim();
        }
        if (image != null && !image.isBlank()) {
            return image.trim();
        }
        return null;
    }
}
