package com.hoadiemcat.entity.enums;

/**
 * Trạng thái của vòng đời mã chuyển/ghép bàn ăn.
 */
public enum TransferStatus {
    /**
     * Đang chờ: Mã đã được sinh và đang chờ nhập xác nhận tại bàn đích (TTL 5 phút).
     */
    PENDING,

    /**
     * Hoàn tất: Đã nhập mã và hoàn thành chuyển giao dữ liệu thành công.
     */
    COMPLETED,

    /**
     * Đã hủy: Người yêu cầu bấm hủy bỏ chuyển bàn tại bàn cũ.
     */
    CANCELLED,

    /**
     * Hết hạn: Quá 5 phút không có thiết bị nào nhập mã xác nhận tại bàn đích.
     */
    EXPIRED
}
