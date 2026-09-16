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

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public JwtAuthResponse login(LoginRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Find the user to return details and update last login
        User user = userRepository.findByEmail(loginRequest.getUsernameOrEmail())
                .orElseGet(() -> userRepository.findByUsername(loginRequest.getUsernameOrEmail())
                        .orElseThrow(() -> new RuntimeException("User not found")));

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token with role claim
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("role", user.getRole().name());
        String token = jwtTokenProvider.generateToken(user.getUsername(), extraClaims);

        java.util.List<String> permissions = switch (user.getRole()) {
            case ADMIN -> java.util.List.of("TABLES", "MENU", "EMPLOYEES", "PROFILE", "TABLES_QR", "INVOICES", "DASHBOARD");
            case MANAGER -> java.util.List.of("TABLES", "MENU");
            case KITCHEN -> java.util.List.of("MENU");
            case STAFF -> java.util.List.of("TABLES");
            default -> java.util.List.of("TABLES");
        };

        // Build UserInfo
        JwtAuthResponse.UserInfo userInfo = JwtAuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .permissions(permissions)
                .build();

        // Build and return response
        return JwtAuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }
}
