package com.riskguard.engine.scorer;

import com.riskguard.engine.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskScorer {

    public int sum(List<RuleEvaluationResult> hitRules) {
        int score = hitRules.stream()
                .mapToInt(rule -> rule.scoreDelta() == null ? 0 : rule.scoreDelta())
                .sum();
        return Math.max(0, Math.min(100, score));
    }
}
