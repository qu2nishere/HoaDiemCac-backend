package com.hoadiemcat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoadiemcat.entity.enums.Role;
import com.hoadiemcat.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể User đại diện cho tài khoản nhân sự trong hệ thống nhà hàng Hỏa Diệm Các.
 * Bao gồm Quản lý (Admin/Manager), Nhân viên bếp (Kitchen) và Nhân viên phục vụ (Staff).
 * Sử dụng để xác thực JWT, phân quyền truy cập hệ thống quản trị và màn hình bếp KDS (UC09).
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class User extends BaseEntity {

    /**
     * Tên đăng nhập duy nhất vào hệ thống quản trị (UC09).
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Mật khẩu đã được mã hóa bằng thuật toán băm an toàn (BCrypt).
     * Được ẩn khi serialize JSON để chống rò rỉ dữ liệu nhạy cảm.
     */
    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * Họ và tên đầy đủ của nhân viên / quản lý hiển thị trên giao diện quản trị.
     */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Địa chỉ email liên hệ duy nhất của nhân sự.
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Số điện thoại liên lạc của nhân sự.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Vai trò phân quyền trong hệ thống: ADMIN, MANAGER, KITCHEN, STAFF, USER (UC09, BR09.1).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Role role = Role.STAFF;

    /**
     * Trạng thái tài khoản: ACTIVE (Hoạt động), INACTIVE (Khóa), PENDING (Chờ duyệt).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /**
     * Thời điểm đăng nhập thành công gần nhất của tài khoản.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
