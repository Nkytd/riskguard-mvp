package com.riskguard.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiskTrendRow {

    private String timeBucket;
    private Long decisionCount;
    private Long highRiskCount;
    private Long criticalRiskCount;
    private Long reviewCount;
    private Long rejectCount;
}
