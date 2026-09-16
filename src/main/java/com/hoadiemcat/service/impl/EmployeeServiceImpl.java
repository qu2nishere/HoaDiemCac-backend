package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.EmployeeCreateRequest;
import com.hoadiemcat.entity.User;
import com.hoadiemcat.entity.enums.Role;
import com.hoadiemcat.entity.enums.Status;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.repository.UserRepository;
import com.hoadiemcat.service.EmployeeService;
import com.hoadiemcat.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    @Transactional
    public User createEmployee(EmployeeCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Email đã tồn tại trong hệ thống");
        }
        
        // Cắt phần tên miền khỏi email để làm username (hoặc lấy trực tiếp email)
        String username = request.getEmail().split("@")[0];
        
        // Đảm bảo username là duy nhất
        int suffix = 1;
        String originalUsername = username;
        while (userRepository.existsByUsername(username)) {
            username = originalUsername + suffix;
            suffix++;
        }

        Role userRole = mapRole(request.getRole());

        User user = User.builder()
                .email(request.getEmail())
                .username(username)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .status(Status.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // Gửi email bất đồng bộ
        mailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), request.getPassword());

        return savedUser;
    }

    @Override
    public java.util.List<com.hoadiemcat.dto.response.EmployeeResponse> getAllEmployees() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.USER)
                .map(this::mapToEmployeeResponse)
                .toList();
    }

    private com.hoadiemcat.dto.response.EmployeeResponse mapToEmployeeResponse(User user) {
        String roleLabel = switch (user.getRole()) {
            case ADMIN -> "Quản Trị Viên (Toàn Quyền)";
            case MANAGER -> "Quản Lý Ca";
            case KITCHEN -> "Bếp / Pha Chế";
            case STAFF -> "Phục Vụ Bàn";
            default -> "Nhân Viên";
        };

        String rolePreset = switch (user.getRole()) {
            case ADMIN -> "all";
            case MANAGER -> "MANAGER";
            case KITCHEN -> "KITCHEN";
            case STAFF -> "SERVER";
            default -> "SERVER";
        };

        java.util.List<String> permissions = switch (user.getRole()) {
            case ADMIN, MANAGER -> java.util.List.of("TABLES", "MENU");
            case KITCHEN -> java.util.List.of("MENU");
            default -> java.util.List.of("TABLES");
        };

        return com.hoadiemcat.dto.response.EmployeeResponse.builder()
                .id(user.getId())
                .code(String.format("NV-%02d", user.getId()))
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .role(roleLabel)
                .roleLabel(roleLabel)
                .rolePreset(rolePreset)
                .permissions(permissions)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private Role mapRole(String roleStr) {
        if (roleStr == null) return Role.STAFF;
        return switch (roleStr) {
            case "Quản Trị Viên (Toàn Quyền)" -> Role.ADMIN;
            case "Quản Lý Ca" -> Role.MANAGER;
            case "Bếp / Pha Chế" -> Role.KITCHEN;
            case "Phục Vụ Bàn", "Thu Ngân" -> Role.STAFF;
            default -> Role.STAFF;
        };
    }
}
