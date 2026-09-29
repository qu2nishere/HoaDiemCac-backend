package com.hoadiemcat.service;

import com.hoadiemcat.dto.response.DashboardSummaryResponse;
import java.time.LocalDate;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary(String period, LocalDate startDate, LocalDate endDate);
}
