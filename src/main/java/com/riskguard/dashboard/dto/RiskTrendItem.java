package com.riskguard.dashboard.dto;

public record RiskTrendItem(
        String timeBucket,
        long decisionCount,
        long highRiskCount,
        long criticalRiskCount,
        long reviewCount,
        long rejectCount
) {
}
