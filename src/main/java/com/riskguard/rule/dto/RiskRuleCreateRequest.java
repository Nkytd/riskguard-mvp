package com.riskguard.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RiskRuleCreateRequest(
        @NotBlank String ruleCode,
        @NotBlank String ruleName,
        @NotBlank String eventType,
        @NotBlank String expression,
        @NotNull Integer score,
        @NotBlank String action,
        Integer priority,
        String description
) {
}
