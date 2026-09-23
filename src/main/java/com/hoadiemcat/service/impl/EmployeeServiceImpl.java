package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.EmployeeCreateRequest;
import com.hoadiemcat.dto.request.EmployeeUpdateRequest;
import com.hoadiemcat.dto.response.EmployeeResponse;
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

import java.util.Arrays;
import java.util.List;

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
        
        // Cắt phần tên miền khỏi email để làm username
        String username = request.getEmail().split("@")[0];
        
        // Đảm bảo username là duy nhất
        int suffix = 1;
        String originalUsername = username;
        while (userRepository.existsByUsername(username)) {
            username = originalUsername + suffix;
            suffix++;
        }

        Role userRole = mapRole(request.getRole());
        String permissionsStr = request.getPermissions() != null && !request.getPermissions().isEmpty()
                ? String.join(",", request.getPermissions())
                : null;

        User user = User.builder()
                .email(request.getEmail())
                .username(username)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .permissions(permissionsStr)
                .status(Status.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // Gửi email bất đồng bộ
        mailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), request.getPassword());

        return savedUser;
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.USER)
                .map(this::mapToEmployeeResponse)
                .toList();
    }

    @Override
    @Transactional
    public User updateEmployee(Long id, EmployeeUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhân viên"));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Email đã tồn tại trong hệ thống");
        }

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhone());

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(mapRole(request.getRole()));
        }

        if (request.getPermissions() != null) {
            user.setPermissions(String.join(",", request.getPermissions()));
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                user.setStatus(Status.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhân viên"));

        user.setStatus(user.getStatus() == Status.ACTIVE ? Status.INACTIVE : Status.ACTIVE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhân viên"));

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhân viên"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private EmployeeResponse mapToEmployeeResponse(User user) {
        String roleLabel = switch (user.getRole()) {
            case ADMIN -> "Quản Trị Viên";
            case MANAGER -> "Quản Lý";
            case KITCHEN -> "Bếp";
            case STAFF -> "Phục Vụ";
            default -> "Nhân Viên";
        };

        String rolePreset = switch (user.getRole()) {
            case ADMIN -> "ADMIN";
            case MANAGER -> "MANAGER";
            case KITCHEN -> "KITCHEN";
            case STAFF -> "STAFF";
            default -> "STAFF";
        };

        List<String> permissions;
        if (user.getPermissions() != null && !user.getPermissions().isBlank()) {
            permissions = Arrays.stream(user.getPermissions().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            permissions = switch (user.getRole()) {
                case ADMIN -> List.of("TABLES", "KITCHEN", "WAITER", "MENU", "EMPLOYEES", "PROFILE", "TABLES_QR", "INVOICES", "DASHBOARD");
                case MANAGER -> List.of("DASHBOARD", "INVOICES", "MENU", "KITCHEN", "WAITER", "TABLES_QR");
                case KITCHEN -> List.of("KITCHEN");
                case STAFF -> List.of("WAITER");
                default -> List.of("WAITER");
            };
        }

        return EmployeeResponse.builder()
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
            case "Quản Trị Viên", "Quản Trị Viên (Toàn Quyền)", "ADMIN", "ROLE_ADMIN" -> Role.ADMIN;
            case "Quản Lý", "Quản Lý Ca", "MANAGER", "ROLE_MANAGER" -> Role.MANAGER;
            case "Bếp", "Bếp / Pha Chế", "KITCHEN", "ROLE_KITCHEN" -> Role.KITCHEN;
            case "Phục Vụ", "Phục Vụ Bàn", "Thu Ngân", "STAFF", "ROLE_STAFF", "SERVER", "CASHIER" -> Role.STAFF;
            default -> Role.STAFF;
        };
    }
}
