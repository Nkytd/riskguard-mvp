package com.riskguard.engine.model;

import java.util.List;

public record DecisionEngineResult(
        Integer rawScore,
        List<RuleEvaluationResult> hitRules
) {
}
