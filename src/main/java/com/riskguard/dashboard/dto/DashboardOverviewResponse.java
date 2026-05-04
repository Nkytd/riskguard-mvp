package com.riskguard.dashboard.dto;

import java.time.LocalDateTime;

public record DashboardOverviewResponse(
        LocalDateTime startTime,
        LocalDateTime endTime,
        long todayDecisionCount,
        long passCount,
        long verifyCount,
        long reviewCount,
        long rejectCount,
        double averageCostMs,
        long highRiskCount,
        long criticalRiskCount,
        long highAndCriticalRiskCount
) {
}
