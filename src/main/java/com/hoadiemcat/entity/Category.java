package com.hoadiemcat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Thực thể Category đại diện cho danh mục món ăn trong thực đơn nhà hàng Hỏa Diệm Các.
 * Hỗ trợ hiển thị menu dạng tab cuộn trên giao diện khách (UC02) và quản lý danh mục (UC10, UC25).
 * Quan hệ Many-to-Many với MenuItem: một món có thể thuộc nhiều danh mục (UC25).
 */
@Entity
@Table(
    name = "categories",
    indexes = {
        @Index(name = "idx_category_slug", columnList = "slug", unique = true),
        @Index(name = "idx_category_display_order", columnList = "display_order"),
        @Index(name = "idx_category_is_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "menuItems")
public class Category extends BaseEntity {

    /**
     * Tên danh mục hiển thị trên tab thực đơn (VD: "Nước Lẩu Hoàng Gia", "Bò Thượng Hạng & Wagyu") (UC02, UC25).
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Đường dẫn định danh dạng slug duy nhất cho danh mục (VD: "nuoc-lau", "bo-wagyu", "ban-chay") (UC25).
     */
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Mô tả chi tiết về danh mục món ăn.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Thứ tự hiển thị ưu tiên của tab danh mục trên giao diện khách hàng (UC25, NFR25.1).
     * Số nhỏ hơn sẽ hiển thị trước.
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Trạng thái kích hoạt hiển thị danh mục trên thực đơn (true = hiển thị, false = ẩn).
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Đánh dấu danh mục đặc biệt do hệ thống tự tổng hợp (ví dụ danh mục "Bán chạy" theo QĐ5, BR25.1).
     * Quản lý không thể gán món thủ công vào danh mục hệ thống này.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    /**
     * Danh sách các món ăn thuộc danh mục này (quan hệ N-N với MenuItem) (UC25).
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<MenuItem> menuItems = new HashSet<>();
}
