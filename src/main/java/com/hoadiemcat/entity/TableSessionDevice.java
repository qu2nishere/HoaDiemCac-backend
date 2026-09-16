package com.hoadiemcat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể TableSessionDevice đại diện cho một thiết bị khách hàng kết nối vào phiên bàn ăn.
 * Quản lý định danh thiết bị (deviceToken), phân quyền Chủ Bàn (isHost) và trạng thái hoạt động (isActive).
 */
@Entity
@Table(
    name = "table_session_devices",
    indexes = {
        @Index(name = "idx_device_token", columnList = "device_token"),
        @Index(name = "idx_device_table_active", columnList = "table_id, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "table")
public class TableSessionDevice extends BaseEntity {

    /**
     * Bàn ăn mà thiết bị này đang kết nối vào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    /**
     * Token định danh thiết bị duy nhất của phiên (UUID v4).
     */
    @Column(name = "device_token", nullable = false, length = 128)
    private String deviceToken;

    /**
     * Tên hiển thị thân thiện của thiết bị (VD: "Thiết bị 1 (Chủ bàn)", "Khách 2 (iPhone)").
     */
    @Column(name = "device_name", length = 100)
    private String deviceName;

    /**
     * Chuỗi nhận dạng thiết bị từ client (User-Agent hoặc Fingerprint).
     */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    /**
     * Đánh dấu thiết bị này có phải là Chủ Bàn (Host) hay không.
     * Chỉ Chủ Bàn mới có quyền bấm "Gửi Bếp" và đá thiết bị khác ra khỏi bàn.
     */
    @Column(name = "is_host", nullable = false)
    @Builder.Default
    private Boolean isHost = false;

    /**
     * Trạng thái thiết bị còn hiệu lực trong phiên bàn ăn hay không.
     * Khi bị Chủ bàn hoặc Thu ngân "Đá ra" (Kick) hoặc bàn thanh toán -> isActive = false.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Thời điểm thiết bị xác thực mã PIN và kết nối vào bàn.
     */
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;
}
