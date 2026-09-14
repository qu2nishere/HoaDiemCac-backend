package com.hoadiemcat.entity.enums;

/**
 * Trạng thái thanh toán của hóa đơn.
 * Liên quan các Use Case: UC15, UC30, UC31, UC32.
 */
public enum PaymentStatus {
    /**
     * Hóa đơn đã được tạo và đang chờ khách thanh toán (tiền mặt hoặc quét mã).
     */
    PENDING,

    /**
     * Thanh toán đã hoàn tất thành công, Quản lý xác nhận thu tiền (UC30, UC31, UC32).
     */
    PAID,

    /**
     * Giao dịch thanh toán bị hủy bỏ do có điều chỉnh lại order hoặc đổi phương thức thanh toán.
     */
    CANCELLED,

    /**
     * Giao dịch chuyển khoản thất bại hoặc quá thời gian chờ.
     */
    FAILED
}
