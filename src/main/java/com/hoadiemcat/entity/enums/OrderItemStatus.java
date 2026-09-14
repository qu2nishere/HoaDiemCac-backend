package com.hoadiemcat.entity.enums;

/**
 * Trạng thái tiến độ chế biến của từng dòng món ăn theo Quy định QĐ8.
 * Đồng bộ theo thời gian thực giữa Khách hàng (UC06), Bếp KDS (UC17, UC18) và Quản lý (UC13).
 */
public enum OrderItemStatus {
    /**
     * Đang chuẩn bị / Đang nấu - Món đang trong hàng đợi hoặc đang được chế biến tại trạm bếp (UC17).
     */
    COOKING,

    /**
     * Đã phục vụ / Đã xong - Món đã được đầu bếp hoàn tất và phục vụ ra bàn khách (UC18).
     */
    SERVED,

    /**
     * Đã hủy - Món bị hủy bởi Quản lý do hết nguyên liệu hoặc khách đổi ý (UC13).
     */
    CANCELLED
}
