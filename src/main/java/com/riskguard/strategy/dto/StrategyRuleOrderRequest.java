package com.riskguard.strategy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StrategyRuleOrderRequest(
        @Valid @NotEmpty List<Item> items
) {

    public record Item(
            @NotNull Long ruleId,
            @NotNull Integer executeOrder
    ) {
    }
}
