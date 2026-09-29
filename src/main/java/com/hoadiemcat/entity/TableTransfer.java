package com.hoadiemcat.entity;

import com.hoadiemcat.entity.enums.TransferStatus;
import com.hoadiemcat.entity.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể TableTransfer đại diện cho một giao dịch chuyển bàn hoặc ghép bàn ăn.
 * Áp dụng cơ chế khóa tạm thời 2 giai đoạn (2-Phase Lock với TTL 5 phút) để bảo toàn giỏ hàng.
 */
@Entity
@Table(
    name = "table_transfers",
    indexes = {
        @Index(name = "idx_transfer_code", columnList = "transfer_code", unique = true),
        @Index(name = "idx_transfer_status_expires", columnList = "status, expires_at"),
        @Index(name = "idx_transfer_source_table", columnList = "source_table_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sourceTable", "targetTable"})
public class TableTransfer extends BaseEntity {

    /**
     * Mã định danh chuyển bàn gồm 6-8 ký tự ngẫu nhiên (VD: "TRF-8492").
     */
    @Column(name = "transfer_code", nullable = false, unique = true, length = 20)
    private String transferCode;

    /**
     * Phân loại hình thức điều chuyển: MOVE (Chuyển bàn 1:1) hoặc MERGE (Ghép bàn N:1).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 20)
    private TransferType transferType;

    /**
     * Bàn nguồn (Bàn cũ nơi xuất phát yêu cầu chuyển/ghép).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_table_id", nullable = false)
    private RestaurantTable sourceTable;

    /**
     * Bàn đích (Bàn mới nhận chuyển hoặc bàn chính nhận ghép, có thể NULL nếu khách chọn sau).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_table_id")
    private RestaurantTable targetTable;

    /**
     * Mã token phiên QR của bàn nguồn tại thời điểm xuất mã.
     */
    @Column(name = "source_session_token", nullable = false, length = 128)
    private String sourceSessionToken;

    /**
     * Mã token phiên QR của bàn đích sau khi hoàn tất điều chuyển.
     */
    @Column(name = "target_session_token", length = 128)
    private String targetSessionToken;

    /**
     * Token thiết bị của người khởi tạo yêu cầu chuyển/ghép bàn.
     */
    @Column(name = "created_by_device", nullable = false, length = 128)
    private String createdByDevice;

    /**
     * Trạng thái giao dịch: PENDING, COMPLETED, CANCELLED, EXPIRED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    /**
     * Thời điểm mã hết hạn (thông thường là createdAt + 5 phút).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Lý do chuyển hoặc ghép bàn (nếu có).
     */
    @Column(name = "reason", length = 255)
    private String reason;

    /**
     * Kiểm tra mã chuyển có bị quá hạn hay chưa.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
