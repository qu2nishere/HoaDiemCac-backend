package com.hoadiemcat.entity;

import com.hoadiemcat.entity.enums.PaymentMethod;
import com.hoadiemcat.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể Invoice đại diện cho hóa đơn thanh toán và kết thúc phiên bàn ăn (UC08, UC15, UC30, UC31, UC32).
 * Áp dụng công thức tính tổng hóa đơn theo Quy định QĐ1 (Tổng tiền món - Chiết khấu + VAT).
 * Lưu trữ lịch sử doanh thu phục vụ báo cáo và thống kê (UC16).
 */
@Entity
@Table(
    name = "invoices",
    indexes = {
        @Index(name = "idx_invoice_code", columnList = "invoice_code", unique = true),
        @Index(name = "idx_invoice_table_id", columnList = "table_id"),
        @Index(name = "idx_invoice_session_token", columnList = "session_token"),
        @Index(name = "idx_invoice_status", columnList = "payment_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"restaurantTable", "cashier"})
public class Invoice extends BaseEntity {

    /**
     * Mã số hóa đơn duy nhất của lượt thanh toán (VD: "HD-20260914-0001") (UC32).
     */
    @Column(name = "invoice_code", nullable = false, unique = true, length = 50)
    private String invoiceCode;

    /**
     * Bàn ăn thực hiện thanh toán hóa đơn này (UC15).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable restaurantTable;

    /**
     * Token phiên QR động của bàn tại thời điểm kết thúc và lập hóa đơn (QĐ2, UC32).
     */
    @Column(name = "session_token", nullable = false, length = 128)
    private String sessionToken;

    /**
     * Tổng tiền các món ăn đã gọi/phục vụ trước khi áp dụng chiết khấu và thuế VAT (VND) (QĐ1).
     */
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    /**
     * Tỷ lệ phần trăm chiết khấu / giảm giá nếu có khuyến mãi (%) (QĐ1).
     */
    @Column(name = "discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /**
     * Số tiền chiết khấu thực tế được trừ vào hóa đơn (VND) (QĐ1).
     */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Tỷ lệ thuế giá trị gia tăng VAT áp dụng (%) (Mặc định 8% hoặc 10%) (QĐ1).
     */
    @Column(name = "tax_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercent = new BigDecimal("8.00");

    /**
     * Số tiền thuế VAT thực tế tính thêm vào hóa đơn (VND) (QĐ1).
     */
    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Tổng số tiền thanh toán cuối cùng của hóa đơn theo công thức QĐ1:
     * finalAmount = subtotal - discountAmount + taxAmount.
     */
    @Column(name = "final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount;

    /**
     * Phương thức thanh toán được áp dụng: CASH (Tiền mặt) hoặc VIETQR (Chuyển khoản Napas247) (UC30, UC31).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /**
     * Trạng thái thanh toán của hóa đơn: PENDING (Chờ thanh toán), PAID (Đã thanh toán), CANCELLED (Đã hủy).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Thời điểm Quản lý/Thu ngân xác nhận đã thu đủ tiền và hoàn tất giao dịch (UC30, UC31).
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Số tiền mặt thực tế khách đưa khi thanh toán bằng tiền mặt (VND) (UC30, NFR30.1).
     */
    @Column(name = "cash_received", precision = 15, scale = 2)
    private BigDecimal cashReceived;

    /**
     * Số tiền thối trả lại cho khách khi thanh toán tiền mặt (VND) (UC30, NFR30.1).
     */
    @Column(name = "cash_change", precision = 15, scale = 2)
    private BigDecimal cashChange;

    /**
     * Mã tham chiếu chuyển khoản hoặc nội dung chuyển khoản VietQR Napas247 phục vụ đối soát (UC31).
     */
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    /**
     * Tài khoản Quản lý / Thu ngân thực hiện xác nhận thanh toán và chốt đóng bàn (UC15, UC32).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    /**
     * Cờ đánh dấu hóa đơn đã được gửi lệnh in ra máy in nhiệt khổ 80mm thành công (UC32, NFR32.1).
     */
    @Column(name = "is_printed", nullable = false)
    @Builder.Default
    private Boolean isPrinted = false;
}
