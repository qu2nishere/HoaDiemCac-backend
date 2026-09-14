package com.hoadiemcat.entity;

import com.hoadiemcat.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Order đại diện cho một đợt gọi món (Order Round) được gửi vào bếp (UC05, UC06, UC13).
 * Mỗi khi khách hàng chốt giỏ hàng và gửi bếp, một Order Round mới được tạo với số thứ tự đợt tăng dần.
 * Quản lý tổng tiền đợt gọi, trạng thái đợt và danh sách các món ăn tương ứng.
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_order_table_id", columnList = "table_id"),
        @Index(name = "idx_order_session_token", columnList = "session_token"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_round_number", columnList = "round_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"restaurantTable", "orderItems"})
public class Order extends BaseEntity {

    /**
     * Bàn ăn thực hiện gửi đợt gọi món này (UC05).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable restaurantTable;

    /**
     * Mã phiên QR của bàn tại thời điểm khách bấm gửi order (QĐ2, UC05).
     */
    @Column(name = "session_token", nullable = false, length = 128)
    private String sessionToken;

    /**
     * Số thứ tự đợt gọi món trong phiên làm việc của bàn (Đợt 1, Đợt 2, Đợt 3...) (UC05, UC17).
     * Phục vụ hiển thị dòng thời gian order (Order Timeline) cho khách và trạm bếp KDS.
     */
    @Column(name = "round_number", nullable = false)
    @Builder.Default
    private Integer roundNumber = 1;

    /**
     * Trạng thái tổng quát của đợt order: PENDING, COOKING, COMPLETED, CANCELLED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Tổng số tiền tạm tính của riêng đợt order này (VND).
     */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Ghi chú chung của khách hàng cho toàn bộ đợt order này.
     */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * Danh sách các dòng món ăn chi tiết trong đợt gọi món này (UC05, UC06).
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Thêm một món vào đợt order và thiết lập quan hệ hai chiều.
     */
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * Xóa một món khỏi đợt order.
     */
    public void removeOrderItem(OrderItem orderItem) {
        orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }
}
