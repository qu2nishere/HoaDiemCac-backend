package com.hoadiemcat.entity.enums;

/**
 * Phân quyền tài khoản trong hệ thống nhà hàng Hỏa Diệm Các.
 * Áp dụng cơ chế RBAC (Role-Based Access Control) cho nhân sự và khách hàng.
 */
public enum Role {
    /**
     * Quản trị viên hệ thống cấp cao nhất, toàn quyền quản trị kỹ thuật và dữ liệu.
     */
    ADMIN,

    /**
     * Quản lý nhà hàng (Manager) - quản lý bàn, menu, can thiệp order, đóng bàn, xem báo cáo doanh thu.
     * Liên quan các UC: UC10, UC11, UC12, UC13, UC14, UC15, UC16.
     */
    MANAGER,

    /**
     * Nhân viên điều phối trạm bếp (Kitchen / KDS) - xem hàng đợi nấu FIFO, cập nhật trạng thái món, báo hết món.
     * Liên quan các UC: UC17, UC18, UC19.
     */
    KITCHEN,

    /**
     * Nhân viên phục vụ bàn (Waiter / Staff) - tiếp nhận chuông gọi phục vụ, hỗ trợ khách tại bàn.
     */
    STAFF,

    /**
     * Khách hàng thông thường hoặc tài khoản mặc định.
     */
    USER
}
