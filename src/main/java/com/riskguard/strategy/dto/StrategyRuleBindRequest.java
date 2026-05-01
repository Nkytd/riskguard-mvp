package com.riskguard.strategy.dto;

import jakarta.validation.constraints.NotNull;

public record StrategyRuleBindRequest(
        @NotNull Long ruleId,
        Integer ruleVersion,
        Integer executeOrder,
        Boolean enabled
) {
}
