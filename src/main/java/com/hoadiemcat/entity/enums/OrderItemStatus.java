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
     * Chờ phục vụ - Món đã được đầu bếp chế biến xong, sẵn sàng tại quầy để nhân viên phục vụ bưng lên bàn (UC18).
     */
    SERVED,

    /**
     * Đã phục vụ - Món đã được nhân viên phục vụ bưng lên bàn cho khách.
     */
    DELIVERED,

    /**
     * Đã hủy - Món bị hủy bởi Quản lý do hết nguyên liệu hoặc khách đổi ý (UC13).
     */
    CANCELLED
}
