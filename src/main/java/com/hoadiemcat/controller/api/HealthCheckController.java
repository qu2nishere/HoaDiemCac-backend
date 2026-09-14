package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "APIs kiểm tra trạng thái hoạt động của hệ thống và Database")
public class HealthCheckController {

    private final DataSource dataSource;

    @GetMapping("/health")
    @Operation(summary = "Kiểm tra hệ thống & Database", description = "Trả về trạng thái hoạt động của backend service và kết nối database Aiven")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "hoadiemcat-backend");
        status.put("status", "UP");
        status.put("version", "v1.0.0");

        boolean dbConnected = false;
        String dbInfo = "Không xác định";
        String dbError = null;

        try (Connection conn = dataSource.getConnection()) {
            dbConnected = conn.isValid(3);
            dbInfo = conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion();
            status.put("databaseCatalog", conn.getCatalog());
        } catch (Exception ex) {
            dbConnected = false;
            dbError = ex.getMessage();
        }

        status.put("database", dbConnected ? "CONNECTED" : "DISCONNECTED");
        status.put("databaseInfo", dbInfo);
        if (dbError != null) {
            status.put("databaseError", dbError);
        }

        String message = dbConnected ? "Hệ thống và Database Aiven kết nối thành công!" : "Hệ thống hoạt động nhưng mất kết nối Database";
        return ResponseEntity.ok(ApiResponse.success(message, status));
    }
}
