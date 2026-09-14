package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Health Check", description = "APIs kiểm tra trạng thái hoạt động của hệ thống")
public class HealthCheckController {

    @GetMapping("/health")
    @Operation(summary = "Kiểm tra hệ thống", description = "Trả về trạng thái hoạt động của backend service")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> status = Map.of(
                "status", "UP",
                "service", "hoadiemcat-backend",
                "version", "v1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.success("Hệ thống hoạt động bình thường", status));
    }
}
