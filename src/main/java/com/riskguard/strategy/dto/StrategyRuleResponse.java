package com.riskguard.strategy.dto;

import com.riskguard.rule.entity.RiskRule;
import com.riskguard.strategy.entity.RiskStrategyRule;

import java.time.LocalDateTime;

public record StrategyRuleResponse(
        Long id,
        Long strategyId,
        Long ruleId,
        String ruleCode,
        String ruleName,
        String eventType,
        Integer ruleVersion,
        Integer executeOrder,
        Boolean enabled,
        LocalDateTime createdAt
) {

    public static StrategyRuleResponse from(RiskStrategyRule relation, RiskRule rule) {
        return new StrategyRuleResponse(
                relation.getId(),
                relation.getStrategyId(),
                relation.getRuleId(),
                rule == null ? null : rule.getRuleCode(),
                rule == null ? null : rule.getRuleName(),
                rule == null ? null : rule.getEventType(),
                relation.getRuleVersion(),
                relation.getExecuteOrder(),
                relation.getEnabled(),
                relation.getCreatedAt()
        );
    }
}
