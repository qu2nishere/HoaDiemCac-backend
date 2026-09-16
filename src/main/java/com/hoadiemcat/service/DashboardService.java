package com.hoadiemcat.service;

import com.hoadiemcat.dto.response.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary(String period);
}
