package com.riskguard.engine.evaluator;

import com.googlecode.aviator.AviatorEvaluator;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.engine.context.RiskContext;
import com.riskguard.engine.model.RuleEvaluationResult;
import com.riskguard.strategy.dto.StrategyRuleSnapshot;
import org.springframework.stereotype.Component;

@Component
public class RuleEvaluator {

    public RuleEvaluationResult evaluate(StrategyRuleSnapshot rule, RiskContext context) {
        try {
            Object result = AviatorEvaluator.execute(rule.expression(), context.variables(), true);
            boolean hit = Boolean.TRUE.equals(result);
            int scoreDelta = hit ? nullToZero(rule.score()) : 0;
            String detail = hit ? "Rule expression matched: " + rule.expression() : "Rule expression not matched";
            return new RuleEvaluationResult(rule, hit, scoreDelta, rule.action(), detail);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Rule execution failed: " + rule.ruleCode() + ", " + ex.getMessage());
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
