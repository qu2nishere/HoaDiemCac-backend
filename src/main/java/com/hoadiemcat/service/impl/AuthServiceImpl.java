package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.LoginRequest;
import com.hoadiemcat.dto.response.JwtAuthResponse;
import com.hoadiemcat.entity.User;
import com.hoadiemcat.repository.UserRepository;
import com.hoadiemcat.security.JwtTokenProvider;
import com.hoadiemcat.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.hoadiemcat.entity.enums.Status;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    @Transactional(noRollbackFor = { AppException.class })
    public JwtAuthResponse login(LoginRequest loginRequest) {
        String usernameOrEmail = loginRequest.getUsernameOrEmail() != null
                ? loginRequest.getUsernameOrEmail().trim()
                : "";

        // 1. Tìm thông tin người dùng trong cơ sở dữ liệu
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElse(null);

        // 2. Nếu tài khoản đã bị khóa hoặc không hoạt động
        if (user != null && user.getStatus() != Status.ACTIVE) {
            log.warn("Đăng nhập bị từ chối: Tài khoản [{}] đang ở trạng thái [{}]", usernameOrEmail, user.getStatus());
            throw new AppException(
                    ErrorCode.UNAUTHENTICATED,
                    "Tài khoản của bạn đã bị tạm khóa, vui lòng liên hệ ban quản lý để mở lại."
            );
        }

        // 3. Tiến hành xác thực đăng nhập qua Spring Security
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            usernameOrEmail,
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException ex) {
            // Sai mật khẩu -> Ghi nhận và tăng số lần sai liên tiếp (chống tấn công brute-force)
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);

                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    user.setStatus(Status.INACTIVE);
                    userRepository.saveAndFlush(user);
                    log.error("🚨 PHÁT HIỆN TẤN CÔNG BRUTE-FORCE: Tài khoản [{}] đã nhập sai mật khẩu {} lần liên tiếp -> Đã chuyển sang TẠM KHÓA (INACTIVE)!",
                            usernameOrEmail, attempts);
                    throw new AppException(
                            ErrorCode.UNAUTHENTICATED,
                            "Tài khoản của bạn đã bị tạm khóa do nhập sai mật khẩu quá 5 lần liên tiếp. Vui lòng liên hệ ban quản lý để mở lại."
                    );
                } else {
                    userRepository.saveAndFlush(user);
                    int remaining = MAX_FAILED_ATTEMPTS - attempts;
                    log.warn("Tài khoản [{}] nhập sai mật khẩu lần {}/{}", usernameOrEmail, attempts, MAX_FAILED_ATTEMPTS);
                    throw new AppException(
                            ErrorCode.UNAUTHENTICATED,
                            "Email hoặc mật khẩu không chính xác. Bạn còn " + remaining + " lần thử trước khi tài khoản bị tạm khóa!"
                    );
                }
            }
            throw new AppException(
                    ErrorCode.UNAUTHENTICATED,
                    "Email hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại!"
            );
        } catch (LockedException | DisabledException ex) {
            throw new AppException(
                    ErrorCode.UNAUTHENTICATED,
                    "Tài khoản của bạn đã bị tạm khóa, vui lòng liên hệ ban quản lý để mở lại."
            );
        }

        // 4. Đăng nhập thành công -> Reset số lần sai về 0 và cập nhật thời điểm đăng nhập
        if (user == null) {
            user = userRepository.findByEmail(usernameOrEmail)
                    .or(() -> userRepository.findByUsername(usernameOrEmail))
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin tài khoản"));
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.saveAndFlush(user);

        // 5. Sinh mã token JWT kèm quyền hạn
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("role", user.getRole().name());
        String token = jwtTokenProvider.generateToken(user.getUsername(), extraClaims);

        JwtAuthResponse.UserInfo userInfo = buildUserInfo(user);

        // 6. Trả về kết quả xác thực
        return JwtAuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }

    @Override
    public JwtAuthResponse.UserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.hoadiemcat.exception.AppException(com.hoadiemcat.exception.ErrorCode.UNAUTHENTICATED, "Chưa xác thực người dùng");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new com.hoadiemcat.exception.AppException(com.hoadiemcat.exception.ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin tài khoản")));

        return buildUserInfo(user);
    }

    private JwtAuthResponse.UserInfo buildUserInfo(User user) {
        java.util.List<String> permissions;
        if (user.getPermissions() != null && !user.getPermissions().isBlank()) {
            permissions = java.util.Arrays.stream(user.getPermissions().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            permissions = switch (user.getRole()) {
                case ADMIN -> java.util.List.of("TABLES", "KITCHEN", "WAITER", "MENU", "EMPLOYEES", "PROFILE", "TABLES_QR", "INVOICES", "DASHBOARD");
                case MANAGER -> java.util.List.of("DASHBOARD", "INVOICES", "MENU", "KITCHEN", "WAITER", "TABLES_QR");
                case KITCHEN -> java.util.List.of("KITCHEN");
                case STAFF -> java.util.List.of("WAITER");
                default -> java.util.List.of("WAITER");
            };
        }

        return JwtAuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .permissions(permissions)
                .build();
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }
}
