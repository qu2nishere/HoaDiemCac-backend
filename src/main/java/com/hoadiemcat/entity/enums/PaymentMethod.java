package com.hoadiemcat.entity.enums;

/**
 * Phương thức thanh toán hóa đơn của nhà hàng Hỏa Diệm Các.
 * Liên quan các Use Case: UC15, UC30, UC31.
 */
public enum PaymentMethod {
    /**
     * Thanh toán bằng tiền mặt trực tiếp tại quầy hoặc tại bàn (UC30).
     */
    CASH,

    /**
     * Thanh toán bằng cách quét mã VietQR động chuẩn Napas247 (UC31).
     */
    VIETQR
}
