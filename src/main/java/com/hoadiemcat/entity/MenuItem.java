package com.hoadiemcat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Thực thể MenuItem đại diện cho một món ăn / thức uống trong thực đơn nhà hàng Hỏa Diệm Các.
 * Quản lý giá bán, tình trạng Còn hàng / Hết hàng theo thời gian thực (QĐ4, UC19, UC26),
 * xóa mềm bảo toàn lịch sử giao dịch (BR24.1) và thống kê lượt gọi phục vụ danh mục Bán chạy (QĐ5).
 */
@Entity
@Table(
    name = "menu_items",
    indexes = {
        @Index(name = "idx_menu_item_code", columnList = "code", unique = true),
        @Index(name = "idx_menu_item_is_available", columnList = "is_available"),
        @Index(name = "idx_menu_item_is_deleted", columnList = "is_deleted"),
        @Index(name = "idx_menu_item_ordered_count", columnList = "total_ordered_count")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "categories")
public class MenuItem extends BaseEntity {

    /**
     * Mã định danh món ăn duy nhất trong thực đơn (VD: "M01", "M02") (UC24).
     */
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    /**
     * Tên gọi của món ăn (VD: "Lẩu 9 Ngăn Trùng Khánh Đại Hồng Bào", "Bò Wagyu A5 Cánh Sen Hoàng Triều") (UC02, UC24).
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Mô tả chi tiết về nguyên liệu, hương vị đặc trưng và hướng dẫn thưởng thức (UC03, UC24).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Đơn giá hiện hành của món ăn tính theo đơn vị VND (UC02, UC24).
     * Sử dụng BigDecimal với độ chính xác cao tránh sai số làm tròn trong tính toán hóa đơn.
     */
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * Đơn vị tính định lượng của món ăn (VD: "Khay 250g", "Nồi 9 ngăn", "Đĩa 200g", "Thuyền lớn") (UC03).
     */
    @Column(name = "unit", length = 50)
    private String unit;

    /**
     * Đường dẫn URL ảnh chụp món ăn chất lượng cao phục vụ hiển thị thực đơn (UC03, UC24, NFR24.1).
     */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /**
     * Trạng thái Còn hàng / Hết hàng của món theo Quy định QĐ4.
     * Có thể được bật/tắt khẩn cấp bởi Bếp (UC19) hoặc Quản lý (UC26) và đồng bộ tức thì tới khách qua WebSocket.
     */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    /**
     * Cờ đánh dấu món ăn đặc sắc, món 'signature' của nhà hàng (Featured Dish) (UC02, UC03).
     */
    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Cờ đánh dấu xóa mềm (Soft Delete) theo Quy định BR24.1.
     * Khi true, món sẽ bị ẩn khỏi thực đơn của khách nhưng vẫn bảo toàn toàn vẹn dữ liệu trong các order lịch sử.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Tổng số lượng món đã được khách đặt thành công trong lịch sử.
     * Sử dụng để tự động xếp hạng Top món bán chạy nhất theo Quy định QĐ5 và báo cáo doanh thu (UC16).
     */
    @Column(name = "total_ordered_count", nullable = false)
    @Builder.Default
    private Long totalOrderedCount = 0L;

    /**
     * Danh sách các danh mục mà món ăn này trực thuộc (quan hệ N-N với Category) (UC25).
     * Bảng trung gian lưu trữ liên kết: menu_item_categories.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "menu_item_categories",
        joinColumns = @JoinColumn(name = "menu_item_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "category_id", referencedColumnName = "id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
