package com.riskguard.engine.resolver;

import com.riskguard.common.enums.RiskDecision;
import com.riskguard.common.enums.RiskLevel;
import com.riskguard.common.enums.RuleAction;
import com.riskguard.engine.model.DecisionOutcome;
import com.riskguard.engine.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DecisionResolver {

    public DecisionOutcome resolve(int riskScore, List<RuleEvaluationResult> hitRules) {
        RiskDecision decision = resolveByAction(hitRules);
        RiskLevel riskLevel = resolveLevel(riskScore);
        if (decision == null) {
            decision = resolveByScore(riskScore);
        }
        return new DecisionOutcome(riskScore, riskLevel.name(), decision.name(), buildReason(hitRules, decision));
    }

    private RiskDecision resolveByAction(List<RuleEvaluationResult> hitRules) {
        if (hasAction(hitRules, RuleAction.REJECT)) {
            return RiskDecision.REJECT;
        }
        if (hasAction(hitRules, RuleAction.REVIEW)) {
            return RiskDecision.REVIEW;
        }
        if (hasAction(hitRules, RuleAction.VERIFY)) {
            return RiskDecision.VERIFY;
        }
        return null;
    }

    private boolean hasAction(List<RuleEvaluationResult> hitRules, RuleAction action) {
        return hitRules.stream().anyMatch(rule -> action.name().equals(rule.action()));
    }

    private RiskDecision resolveByScore(int score) {
        if (score >= 81) {
            return RiskDecision.REJECT;
        }
        if (score >= 61) {
            return RiskDecision.REVIEW;
        }
        if (score >= 31) {
            return RiskDecision.VERIFY;
        }
        return RiskDecision.PASS;
    }

    private RiskLevel resolveLevel(int score) {
        if (score >= 81) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 61) {
            return RiskLevel.HIGH;
        }
        if (score >= 31) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String buildReason(List<RuleEvaluationResult> hitRules, RiskDecision decision) {
        if (hitRules.isEmpty()) {
            return "No risk rules hit, final decision is " + decision.name();
        }
        String ruleNames = hitRules.stream()
                .map(rule -> rule.rule().ruleName())
                .collect(Collectors.joining(", "));
        return "Hit rules: " + ruleNames + ", final decision is " + decision.name();
    }
}
