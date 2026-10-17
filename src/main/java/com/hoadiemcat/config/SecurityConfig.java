package com.hoadiemcat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.security.JwtAuthenticationEntryPoint;
import com.hoadiemcat.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/public/health",
            "/api/v1/customer/**",
            "/api/v1/kitchen/**",
            "/api/v1/menu/**",
            "/api/v1/admin/tables/**",
            "/api/v1/admin/orders/**",
            "/ws/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            ApiResponse<Object> apiResponse = ApiResponse.builder()
                    .code(ErrorCode.UNAUTHORIZED.getCode())
                    .message(ErrorCode.UNAUTHORIZED.getMessage())
                    .build();
            new ObjectMapper().findAndRegisterModules().writeValue(response.getWriter(), apiResponse);
            response.flushBuffer();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {}) // Uses CorsFilter bean
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Phân quyền tải ảnh lên CDN (UC41) - chỉ ADMIN hoặc MANAGER
                        .requestMatchers("/api/v1/public/upload/**", "/api/v1/upload/**").hasAnyRole("ADMIN", "MANAGER")

                        // 2. Phân quyền quản lý món ăn và danh mục thực đơn - chỉ ADMIN hoặc MANAGER
                        .requestMatchers(HttpMethod.POST, "/api/v1/menu-items", "/api/v1/menu-items/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/menu-items/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/menu-items/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/menu-items/**").hasAnyRole("ADMIN", "MANAGER")

                        // 3. Khách hàng và nhân viên có thể xem thực đơn món ăn (GET) công khai
                        .requestMatchers(HttpMethod.GET, "/api/v1/menu-items", "/api/v1/menu-items/**").permitAll()

                        // 4. Các endpoint công khai còn lại
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Tất cả request khác yêu cầu đã xác thực
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
