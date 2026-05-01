package com.riskguard.engine;

import com.riskguard.engine.context.RiskContext;
import com.riskguard.engine.evaluator.RuleEvaluator;
import com.riskguard.engine.model.ActiveStrategySnapshot;
import com.riskguard.engine.model.DecisionEngineResult;
import com.riskguard.engine.resolver.DecisionResolver;
import com.riskguard.engine.scorer.RiskScorer;
import com.riskguard.strategy.dto.StrategyRuleSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTests {

    @Test
    void executeRulesWithNestedContext() {
        DecisionEngine engine = new DecisionEngine(new RuleEvaluator(), new RiskScorer());
        ActiveStrategySnapshot strategy = new ActiveStrategySnapshot(1L, 1, 100, List.of(
                new StrategyRuleSnapshot(1L, "NEW_USER_HIGH_AMOUNT", "新用户大额支付", "PAYMENT", 1,
                        "user.registerDays <= 7 && event.amount >= 1000", 30, "SCORE", 100, 100),
                new StrategyRuleSnapshot(2L, "DEVICE_MULTI_ACCOUNT", "设备关联账号过多", "PAYMENT", 1,
                        "device.accountCount24h >= 5", 40, "SCORE", 110, 110)
        ));
        RiskContext context = new RiskContext(Map.of(
                "event", Map.of("amount", 1299D),
                "user", Map.of("registerDays", 3),
                "device", Map.of("accountCount24h", 6)
        ));

        DecisionEngineResult result = engine.execute(strategy, context);

        assertThat(result.rawScore()).isEqualTo(70);
        assertThat(result.hitRules()).hasSize(2);
    }

    @Test
    void resolveReviewDecisionByScore() {
        DecisionResolver resolver = new DecisionResolver();
        var outcome = resolver.resolve(70, List.of());

        assertThat(outcome.riskLevel()).isEqualTo("HIGH");
        assertThat(outcome.decision()).isEqualTo("REVIEW");
    }
}
