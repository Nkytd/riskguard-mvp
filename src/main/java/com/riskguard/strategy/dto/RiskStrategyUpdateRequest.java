package com.riskguard.strategy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RiskStrategyUpdateRequest(
        @NotBlank String strategyName,
        @NotBlank String eventType,
        @Min(0) @Max(100) Integer grayRatio,
        String description
) {
}
