package com.riskguard.rule.dto;

import com.riskguard.rule.entity.RiskRuleVersion;

import java.time.LocalDateTime;

public record RuleVersionResponse(
        Long id,
        Long ruleId,
        Integer version,
        String expression,
        Integer score,
        String action,
        Integer priority,
        String status,
        String publishNote,
        LocalDateTime createdAt
) {

    public static RuleVersionResponse from(RiskRuleVersion version) {
        return new RuleVersionResponse(
                version.getId(),
                version.getRuleId(),
                version.getVersion(),
                version.getExpressionText(),
                version.getScore(),
                version.getAction(),
                version.getPriority(),
                version.getStatus(),
                version.getPublishNote(),
                version.getCreatedAt()
        );
    }
}
