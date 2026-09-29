package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.DashboardSummaryResponse;
import com.hoadiemcat.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard API", description = "Báo cáo doanh thu và phân tích hiệu quả kinh doanh")
public class DashboardAdminController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Lấy báo cáo tổng hợp doanh thu, KPI, biểu đồ, top món ăn và lịch sử hóa đơn theo kỳ (today/day, week, month, year, custom)")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(period, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
