package com.riskguard.engine;

import com.riskguard.engine.context.RiskContext;
import com.riskguard.engine.evaluator.RuleEvaluator;
import com.riskguard.engine.model.ActiveStrategySnapshot;
import com.riskguard.engine.model.DecisionEngineResult;
import com.riskguard.engine.model.RuleEvaluationResult;
import com.riskguard.engine.scorer.RiskScorer;
import com.riskguard.strategy.dto.StrategyRuleSnapshot;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DecisionEngine {

    private final RuleEvaluator ruleEvaluator;
    private final RiskScorer riskScorer;

    public DecisionEngine(RuleEvaluator ruleEvaluator, RiskScorer riskScorer) {
        this.ruleEvaluator = ruleEvaluator;
        this.riskScorer = riskScorer;
    }

    public DecisionEngineResult execute(ActiveStrategySnapshot strategy, RiskContext context) {
        List<RuleEvaluationResult> hitRules = strategy.rules()
                .stream()
                .sorted(Comparator.comparing(StrategyRuleSnapshot::executeOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(StrategyRuleSnapshot::priority, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(StrategyRuleSnapshot::ruleId))
                .map(rule -> ruleEvaluator.evaluate(rule, context))
                .filter(RuleEvaluationResult::hit)
                .toList();

        return new DecisionEngineResult(riskScorer.sum(hitRules), hitRules);
    }
}
