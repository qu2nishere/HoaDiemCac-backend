package com.hoadiemcat.entity.enums;

/**
 * Trạng thái xử lý của chuông gọi nhân viên / yêu cầu hỗ trợ từ bàn.
 * Liên quan các Use Case: UC07, UC14.
 */
public enum CallStaffStatus {
    /**
     * Yêu cầu mới được tạo từ bàn khách, đang chờ nhân viên/quản lý tiếp nhận (UC07, UC14).
     */
    PENDING,

    /**
     * Nhân viên đã tiếp nhận và xử lý xong yêu cầu của khách tại bàn (UC14).
     */
    RESOLVED,

    /**
     * Yêu cầu bị hủy bỏ (ví dụ khách bấm nhầm hoặc bàn đóng trước khi kịp xử lý).
     */
    CANCELLED
}
