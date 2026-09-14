package com.hoadiemcat.entity;

import com.hoadiemcat.entity.enums.TableArea;
import com.hoadiemcat.entity.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể RestaurantTable đại diện cho một bàn ăn trong nhà hàng Hỏa Diệm Các.
 * Quản lý tình trạng bàn (QĐ7), sơ đồ mặt bằng trực quan (UC12), cờ khóa order khẩn cấp (QĐ3, UC29)
 * và vòng đời mã QR động / Dynamic Session Token chống lộ QR (QĐ2, UC01, UC28, UC32).
 */
@Entity
@Table(
    name = "restaurant_tables",
    indexes = {
        @Index(name = "idx_table_number", columnList = "table_number", unique = true),
        @Index(name = "idx_table_session_token", columnList = "current_session_token"),
        @Index(name = "idx_table_status", columnList = "status"),
        @Index(name = "idx_table_area", columnList = "area")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RestaurantTable extends BaseEntity {

    /**
     * Mã định danh bàn duy nhất trong nhà hàng (VD: "B01", "B02", "VIP11") (UC27).
     */
    @Column(name = "table_number", nullable = false, unique = true, length = 20)
    private String tableNumber;

    /**
     * Tên hiển thị thân thiện trên sơ đồ mặt bằng và hóa đơn (VD: "Bàn 01", "Phòng VIP Hoàng Triều") (UC12, UC27).
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Phân khu vực bố trí bàn: COMMON (Khu sảnh chung), VIP (Phòng VIP Hoàng Gia) (UC27).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false, length = 30)
    @Builder.Default
    private TableArea area = TableArea.COMMON;

    /**
     * Sức chứa tối đa (số lượng khách tối đa có thể ngồi tại bàn) (UC27).
     */
    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 4;

    /**
     * Trạng thái vận hành của bàn ăn theo QĐ7: AVAILABLE (Trống), OCCUPIED (Có khách), CLEANING (Dọn dẹp).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    /**
     * Cờ kiểm soát khóa order khẩn cấp của bàn theo Quy định QĐ3 và UC29.
     * Khi true, khách tại bàn không thể bấm "Gửi đơn vào bếp" hay thêm món (HTTP 403).
     */
    @Column(name = "is_order_locked", nullable = false)
    @Builder.Default
    private Boolean isOrderLocked = false;

    /**
     * Token phiên QR động hiện hành của bàn (UUID v4 kết hợp băm an toàn theo QĐ2).
     * Dùng để định danh và xác thực phiên của khách khi quét QR (UC01).
     * Bị thu hồi (revoke) khi đóng bàn (UC32) hoặc cấp lại mã QR mới (UC28).
     */
    @Column(name = "current_session_token", length = 128)
    private String currentSessionToken;

    /**
     * Thời điểm khách hàng đầu tiên quét QR hợp lệ để mở bàn chuyển sang trạng thái OCCUPIED (UC01, UC12).
     * Phục vụ tính thời gian ngồi ăn (elapsed time) trên sơ đồ bàn.
     */
    @Column(name = "session_started_at")
    private LocalDateTime sessionStartedAt;

    /**
     * Đường dẫn URL ảnh mã QR chứa Dynamic Session Token phục vụ in ấn hoặc hiển thị (UC27, UC28).
     */
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;
}
