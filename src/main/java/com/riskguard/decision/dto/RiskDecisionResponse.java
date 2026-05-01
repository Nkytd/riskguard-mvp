package com.riskguard.decision.dto;

import java.util.List;

public record RiskDecisionResponse(
        String decisionNo,
        String eventNo,
        String caseNo,
        String decision,
        Integer riskScore,
        String riskLevel,
        Long strategyId,
        Integer strategyVersion,
        List<DecisionHitRuleResponse> hitRules,
        String reason,
        Long costMs,
        Boolean idempotent
) {
}
