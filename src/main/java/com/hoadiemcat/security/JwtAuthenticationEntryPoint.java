package com.hoadiemcat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .code(ErrorCode.UNAUTHENTICATED.getCode())
                .message(ErrorCode.UNAUTHENTICATED.getMessage())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
