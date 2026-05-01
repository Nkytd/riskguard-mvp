package com.riskguard.strategy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StrategyRollbackRequest(
        @NotNull @Positive Integer version
) {
}
