package com.riskguard.engine.model;

public record DecisionOutcome(
        Integer riskScore,
        String riskLevel,
        String decision,
        String reason
) {
}
