package com.riskguard.decision.dto;

public record DecisionHitRuleResponse(
        Long ruleId,
        String ruleCode,
        String ruleName,
        Integer ruleVersion,
        Integer scoreDelta,
        String action,
        String hitDetail
) {
}
