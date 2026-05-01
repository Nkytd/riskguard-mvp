package com.riskguard.engine.model;

import com.riskguard.strategy.dto.StrategyRuleSnapshot;

import java.util.List;

public record ActiveStrategySnapshot(
        Long strategyId,
        Integer strategyVersion,
        Integer grayRatio,
        List<StrategyRuleSnapshot> rules
) {
}
