package com.hoadiemcat.entity;

import com.hoadiemcat.entity.enums.OrderItemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể OrderItem đại diện cho một dòng món ăn đã được gửi vào bếp chế biến (UC05, UC06).
 * Quản lý đơn giá tại thời điểm gọi, trạng thái chế biến theo Quy định QĐ8 (COOKING, SERVED, CANCELLED),
 * thời điểm bếp hoàn tất ra món (UC18) và lý do can thiệp hủy món của Quản lý (UC13).
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_item_order_id", columnList = "order_id"),
        @Index(name = "idx_order_item_menu_item_id", columnList = "menu_item_id"),
        @Index(name = "idx_order_item_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "menuItem"})
public class OrderItem extends BaseEntity {

    /**
     * Đợt order cha chứa dòng món ăn này (UC05).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Món ăn được đặt từ thực đơn (UC05).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    /**
     * Đơn giá món ăn tại thời điểm khách bấm gọi (VND) (UC05).
     * Lưu trữ cố định để đảm bảo hóa đơn không bị ảnh hưởng nếu sau này giá thực đơn thay đổi.
     */
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * Số lượng phần ăn được đặt (1 <= quantity <= 99 theo Quy định QĐ9).
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Thành tiền cho dòng món này = price * quantity (VND).
     */
    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Trạng thái tiến độ chế biến của món theo Quy định QĐ8:
     * COOKING (Đang chuẩn bị), SERVED (Đã phục vụ), CANCELLED (Đã hủy).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.COOKING;

    /**
     * Ghi chú hương vị hoặc yêu cầu đặc biệt của khách cho món này (VD: "Không hành", "Ít sa tế").
     */
    @Column(name = "note", length = 255)
    private String note;

    /**
     * Thời điểm nhân viên bếp hoàn tất chế biến và đánh dấu phục vụ ra bàn (UC18).
     * Dùng để tính toán thời gian chờ thực tế (elapsed time) trên màn hình KDS trạm bếp.
     */
    @Column(name = "served_at")
    private LocalDateTime servedAt;

    /**
     * Thời điểm món ăn bị hủy bởi Quản lý (nếu có sự cố xảy ra) (UC13).
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Lý do hủy món được Quản lý ghi nhận (bắt buộc khi hủy món theo UC13, BR13.1).
     */
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}
