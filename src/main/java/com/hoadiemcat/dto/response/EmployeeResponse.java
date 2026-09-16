package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String code;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String roleLabel;
    private String rolePreset;
    private List<String> permissions;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
