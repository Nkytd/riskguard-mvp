package com.riskguard.dashboard.dto;

public record DashboardDistributionItem(
        String name,
        long count,
        double ratio
) {
}
