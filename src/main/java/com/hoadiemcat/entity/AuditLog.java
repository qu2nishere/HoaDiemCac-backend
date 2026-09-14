package com.hoadiemcat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể AuditLog đại diện cho nhật ký can thiệp và điều chỉnh nghiệp vụ của Quản lý (UC13, BR13.1).
 * Ghi lại minh bạch mọi thao tác sửa số lượng, đổi món, hủy món hoặc khóa bàn kèm lý do bắt buộc.
 * Phục vụ truy vết sự cố, đối soát doanh thu và kiểm toán vận hành nhà hàng.
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_actor_id", columnList = "actor_id"),
        @Index(name = "idx_audit_target", columnList = "target_entity, target_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "actor")
public class AuditLog extends BaseEntity {

    /**
     * Tài khoản Quản lý / Nhân sự thực hiện hành động can thiệp (UC13).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /**
     * Tên hành động can thiệp (VD: "CANCEL_ORDER_ITEM", "UPDATE_QUANTITY", "LOCK_ORDER", "REVOKE_QR") (UC13).
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /**
     * Tên thực thể / bảng dữ liệu bị tác động (VD: "OrderItem", "Order", "RestaurantTable").
     */
    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;

    /**
     * Khóa chính ID của bản ghi dữ liệu bị can thiệp.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * Lý do bắt buộc phải nhập khi Quản lý can thiệp vào order của khách (Quy định BR13.1, UC13).
     * (VD: "Khách đổi ý sang lẩu nấm", "Bếp cạn nguyên liệu bò Wagyu đột xuất").
     */
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /**
     * Giá trị cũ trước khi bị can thiệp (dưới dạng chuỗi hoặc JSON snapshot).
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * Giá trị mới sau khi can thiệp điều chỉnh (dưới dạng chuỗi hoặc JSON snapshot).
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
}
