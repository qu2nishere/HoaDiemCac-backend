package com.hoadiemcat.entity;

import com.hoadiemcat.entity.enums.CallStaffStatus;
import com.hoadiemcat.entity.enums.CallStaffType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể CallStaffLog đại diện cho nhật ký chuông gọi nhân viên và yêu cầu hỗ trợ từ bàn khách (UC07, UC14).
 * Giúp thống kê thời gian phản hồi của nhân sự, cảnh báo âm thanh realtime tới màn hình Quản lý
 * và kiểm soát tần suất gọi chuông (Rate Limiting 30 giây theo Quy định QĐ10).
 */
@Entity
@Table(
    name = "call_staff_logs",
    indexes = {
        @Index(name = "idx_call_staff_table_id", columnList = "table_id"),
        @Index(name = "idx_call_staff_status", columnList = "status"),
        @Index(name = "idx_call_staff_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"restaurantTable", "resolvedBy"})
public class CallStaffLog extends BaseEntity {

    /**
     * Bàn ăn phát tín hiệu gọi phục vụ (UC07).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable restaurantTable;

    /**
     * Phân loại yêu cầu hỗ trợ từ khách: CALL_STAFF, PAYMENT_REQUEST, ICE_WATER, UTENSILS, OTHER (UC07, UC08).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    @Builder.Default
    private CallStaffType requestType = CallStaffType.CALL_STAFF;

    /**
     * Nội dung chi tiết hoặc ghi chú hỗ trợ mà khách gửi kèm.
     */
    @Column(name = "message", length = 255)
    private String message;

    /**
     * Trạng thái xử lý của yêu cầu: PENDING (Đang chờ tiếp nhận), RESOLVED (Đã xử lý xong), CANCELLED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CallStaffStatus status = CallStaffStatus.PENDING;

    /**
     * Thời điểm nhân viên xác nhận đã tiếp cận bàn và giải quyết xong yêu cầu (UC14).
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Tài khoản nhân viên / quản lý đã tiếp nhận và xử lý yêu cầu hỗ trợ này (UC14).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;
}
