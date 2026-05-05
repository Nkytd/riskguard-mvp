package com.riskguard.dashboard.dto;

public record CaseStatisticsResponse(
        long totalCount,
        long pendingCount,
        long processingCount,
        long approvedCount,
        long rejectedCount,
        long closedCount,
        double approvalRate,
        double rejectionRate
) {
}
