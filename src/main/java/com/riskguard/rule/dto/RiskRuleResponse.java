package com.riskguard.rule.dto;

import com.riskguard.rule.entity.RiskRule;

import java.time.LocalDateTime;

public record RiskRuleResponse(
        Long id,
        String ruleCode,
        String ruleName,
        String eventType,
        String expression,
        Integer score,
        String action,
        Integer priority,
        String status,
        Integer version,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RiskRuleResponse from(RiskRule rule) {
        return new RiskRuleResponse(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getEventType(),
                rule.getExpressionText(),
                rule.getScore(),
                rule.getAction(),
                rule.getPriority(),
                rule.getStatus(),
                rule.getVersion(),
                rule.getDescription(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
