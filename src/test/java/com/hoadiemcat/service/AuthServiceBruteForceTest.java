package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.LoginRequest;
import com.hoadiemcat.dto.response.JwtAuthResponse;
import com.hoadiemcat.entity.User;
import com.hoadiemcat.entity.enums.Role;
import com.hoadiemcat.entity.enums.Status;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.repository.UserRepository;
import com.hoadiemcat.security.JwtTokenProvider;
import com.hoadiemcat.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceBruteForceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("kqtthings")
                .email("kqtthings@gmail.com")
                .password("$2a$10$hashedpassword")
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .failedLoginAttempts(0)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("kqtthings@gmail.com");
        loginRequest.setPassword("WrongPass123");
    }

    @Test
    @DisplayName("TC_BF_01: Nhập sai mật khẩu lần 1 -> failedLoginAttempts tăng lên 1, tài khoản vẫn ACTIVE")
    void testFailedLoginAttempt_Once() {
        when(userRepository.findByEmail("kqtthings@gmail.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AppException exception = assertThrows(AppException.class, () -> authService.login(loginRequest));

        assertEquals(1, testUser.getFailedLoginAttempts());
        assertEquals(Status.ACTIVE, testUser.getStatus());
        assertTrue(exception.getMessage().contains("Bạn còn 4 lần thử"));
        verify(userRepository, times(1)).saveAndFlush(testUser);
    }

    @Test
    @DisplayName("TC_BF_02: Nhập sai mật khẩu liên tiếp đến lần thứ 5 -> Khóa tài khoản (Status.INACTIVE)")
    void testFailedLoginAttempt_ReachingFiveTimesLocksAccount() {
        testUser.setFailedLoginAttempts(4); // Đã sai 4 lần trước đó
        when(userRepository.findByEmail("kqtthings@gmail.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AppException exception = assertThrows(AppException.class, () -> authService.login(loginRequest));

        assertEquals(5, testUser.getFailedLoginAttempts());
        assertEquals(Status.INACTIVE, testUser.getStatus()); // Tài khoản đã chuyển sang Tạm Khóa!
        assertTrue(exception.getMessage().contains("tạm khóa do nhập sai mật khẩu quá 5 lần liên tiếp"));
        verify(userRepository, times(1)).saveAndFlush(testUser);
    }

    @Test
    @DisplayName("TC_BF_03: Tài khoản đã bị tạm khóa (INACTIVE) -> Chặn đăng nhập ngay lập tức")
    void testLockedAccountCannotLogin() {
        testUser.setStatus(Status.INACTIVE);
        when(userRepository.findByEmail("kqtthings@gmail.com")).thenReturn(Optional.of(testUser));

        AppException exception = assertThrows(AppException.class, () -> authService.login(loginRequest));

        assertTrue(exception.getMessage().contains("tạm khóa"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("TC_BF_04: Đăng nhập thành công -> Reset failedLoginAttempts về 0")
    void testSuccessfulLoginResetsFailedAttempts() {
        testUser.setFailedLoginAttempts(3); // Từng nhập sai 3 lần
        loginRequest.setPassword("Abc@1234"); // Mật khẩu đúng

        Authentication auth = mock(Authentication.class);
        when(userRepository.findByEmail("kqtthings@gmail.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generateToken(eq(testUser.getUsername()), any())).thenReturn("mock-jwt-token");

        JwtAuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals(0, testUser.getFailedLoginAttempts()); // Đã reset về 0
        verify(userRepository, times(1)).saveAndFlush(testUser);
    }
}
