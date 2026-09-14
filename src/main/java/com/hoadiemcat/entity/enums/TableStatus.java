package com.hoadiemcat.entity.enums;

/**
 * Trạng thái vận hành của bàn ăn theo Quy định QĐ7.
 * Hệ thống cố định 3 trạng thái bàn để phục vụ quản lý sơ đồ bàn trực quan (UC12).
 */
public enum TableStatus {
    /**
     * Bàn trống - Sẵn sàng đón lượt khách mới quét mã QR (UC01).
     */
    AVAILABLE,

    /**
     * Đang có khách - Bàn đã được khách quét mã QR thành công và đang phục vụ (UC01, UC12).
     */
    OCCUPIED,

    /**
     * Đang dọn dẹp - Bàn vừa thanh toán và đóng bàn xong, nhân viên đang dọn dẹp vệ sinh (UC32).
     */
    CLEANING
}
