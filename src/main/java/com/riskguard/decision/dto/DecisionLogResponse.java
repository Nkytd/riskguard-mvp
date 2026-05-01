package com.riskguard.decision.dto;

import com.riskguard.decision.entity.RiskDecisionLog;

import java.time.LocalDateTime;
import java.util.List;

public record DecisionLogResponse(
        String decisionNo,
        String eventNo,
        String requestNo,
        String eventType,
        String userId,
        Long strategyId,
        Integer strategyVersion,
        Integer riskScore,
        String riskLevel,
        String decision,
        String reason,
        Integer costMs,
        String traceId,
        LocalDateTime createdAt,
        List<DecisionHitRuleResponse> hitRules
) {

    public static DecisionLogResponse from(RiskDecisionLog log, List<DecisionHitRuleResponse> hitRules) {
        return new DecisionLogResponse(
                log.getDecisionNo(),
                log.getEventNo(),
                log.getRequestNo(),
                log.getEventType(),
                log.getUserId(),
                log.getStrategyId(),
                log.getStrategyVersion(),
                log.getRiskScore(),
                log.getRiskLevel(),
                log.getDecision(),
                log.getReason(),
                log.getCostMs(),
                log.getTraceId(),
                log.getCreatedAt(),
                hitRules
        );
    }
}
