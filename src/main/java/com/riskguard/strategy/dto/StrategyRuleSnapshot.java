package com.riskguard.strategy.dto;

public record StrategyRuleSnapshot(
        Long ruleId,
        String ruleCode,
        String ruleName,
        String eventType,
        Integer ruleVersion,
        String expression,
        Integer score,
        String action,
        Integer priority,
        Integer executeOrder
) {
}
