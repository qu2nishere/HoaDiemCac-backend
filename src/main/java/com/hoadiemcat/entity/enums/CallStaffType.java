package com.hoadiemcat.entity.enums;

/**
 * Phân loại yêu cầu hỗ trợ hoặc chuông gọi nhân viên từ khách tại bàn.
 * Liên quan các Use Case: UC07, UC08, UC14.
 */
public enum CallStaffType {
    /**
     * Chuông gọi nhân viên chung hỗ trợ tại bàn (UC07).
     */
    CALL_STAFF,

    /**
     * Yêu cầu thanh toán hóa đơn tạm tính từ bàn (UC08, UC14).
     */
    PAYMENT_REQUEST,

    /**
     * Yêu cầu châm thêm nước hoặc xin thêm đá lạnh.
     */
    ICE_WATER,

    /**
     * Yêu cầu thêm chén, bát, đũa, muỗng hoặc khăn ướt.
     */
    UTENSILS,

    /**
     * Các yêu cầu hỗ trợ khác.
     */
    OTHER
}
