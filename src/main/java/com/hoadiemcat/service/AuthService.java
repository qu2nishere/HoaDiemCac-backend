package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.LoginRequest;
import com.hoadiemcat.dto.response.JwtAuthResponse;

public interface AuthService {
    JwtAuthResponse login(LoginRequest loginRequest);
}
