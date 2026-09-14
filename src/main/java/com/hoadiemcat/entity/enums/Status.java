package com.hoadiemcat.entity.enums;

/**
 * Trạng thái hoạt động của tài khoản người dùng trong hệ thống.
 */
public enum Status {
    /**
     * Tài khoản đang hoạt động bình thường, có quyền đăng nhập.
     */
    ACTIVE,

    /**
     * Tài khoản bị tạm khóa, không thể đăng nhập hoặc thực hiện thao tác.
     */
    INACTIVE,

    /**
     * Tài khoản đang chờ duyệt kích hoạt.
     */
    PENDING,

    /**
     * Tài khoản đã bị xóa (xóa mềm).
     */
    DELETED
}
