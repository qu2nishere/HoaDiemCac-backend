package com.hoadiemcat.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên quá dài")
    private String fullName;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^[0-9]{9,11}$", message = "Số điện thoại chỉ được chứa ký tự số (9 đến 11 chữ số)")
    private String phone;

    private String role;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^.{8,}$", message = "Mật khẩu phải từ 8 ký tự trở lên")
    private String password;

    private List<String> permissions;

    private String status;
}
