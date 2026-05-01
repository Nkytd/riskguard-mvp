package com.riskguard.engine.model;

import com.riskguard.strategy.dto.StrategyRuleSnapshot;

public record RuleEvaluationResult(
        StrategyRuleSnapshot rule,
        boolean hit,
        Integer scoreDelta,
        String action,
        String detail
) {
}
