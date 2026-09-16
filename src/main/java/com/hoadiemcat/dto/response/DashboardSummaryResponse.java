package com.hoadiemcat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal totalRevenue;
    private Long totalInvoices;
    private BigDecimal averageOrderValue;
    private Double occupancyRate;
    private BigDecimal cashRevenue;
    private BigDecimal vietQrRevenue;
    private Long cashInvoicesCount;
    private Long vietQrInvoicesCount;
    private Integer availableTables;
    private Integer occupiedTables;
    private Integer totalTables;
    private List<RevenueChartPoint> revenueChart;
    private List<TopSellingDish> topSellingDishes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueChartPoint {
        private String label;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingDish {
        private String dishName;
        private String category;
        private Long quantitySold;
        private BigDecimal revenue;
        private String imageUrl;
    }
}
