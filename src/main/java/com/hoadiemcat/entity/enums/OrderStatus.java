package com.hoadiemcat.entity.enums;

/**
 * Trạng thái tổng quát của một đợt gọi món (Order Round) từ bàn gửi vào bếp.
 * Liên quan các Use Case: UC05, UC06, UC13, UC17.
 */
public enum OrderStatus {
    /**
     * Đợt order vừa được khách gửi từ giỏ hàng, đang chờ bếp hoặc hệ thống tiếp nhận (UC05).
     */
    PENDING,

    /**
     * Bếp đã tiếp nhận và đang tiến hành chế biến các món trong đợt (UC17).
     */
    COOKING,

    /**
     * Tất cả các món trong đợt order đã được chế biến và phục vụ ra bàn thành công.
     */
    COMPLETED,

    /**
     * Toàn bộ đợt order đã bị hủy bởi Quản lý (UC13).
     */
    CANCELLED
}
