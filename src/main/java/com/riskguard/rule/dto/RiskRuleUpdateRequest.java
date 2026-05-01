package com.riskguard.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RiskRuleUpdateRequest(
        @NotBlank String ruleName,
        @NotBlank String eventType,
        @NotBlank String expression,
        @NotNull Integer score,
        @NotBlank String action,
        Integer priority,
        String description
) {
}
