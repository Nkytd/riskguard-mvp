package com.riskguard.decision.service;

import com.riskguard.common.cache.RiskCacheService;
import com.riskguard.common.enums.EventType;
import com.riskguard.common.utils.EnumUtils;
import com.riskguard.common.utils.NoGenerator;
import com.riskguard.decision.dto.DecisionHitRuleResponse;
import com.riskguard.decision.dto.RiskDecisionRequest;
import com.riskguard.decision.dto.RiskDecisionResponse;
import com.riskguard.engine.DecisionEngine;
import com.riskguard.engine.context.ActiveStrategyLoader;
import com.riskguard.engine.context.RiskContext;
import com.riskguard.engine.context.RiskContextBuilder;
import com.riskguard.engine.model.ActiveStrategySnapshot;
import com.riskguard.engine.model.DecisionEngineResult;
import com.riskguard.engine.model.DecisionOutcome;
import com.riskguard.engine.model.RuleEvaluationResult;
import com.riskguard.engine.resolver.DecisionResolver;
import com.riskguard.riskcase.service.RiskCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RiskDecisionService {

    private final RiskCacheService riskCacheService;
    private final ActiveStrategyLoader activeStrategyLoader;
    private final RiskContextBuilder riskContextBuilder;
    private final DecisionEngine decisionEngine;
    private final DecisionResolver decisionResolver;
    private final DecisionPersistenceService decisionPersistenceService;
    private final RiskCaseService riskCaseService;

    public RiskDecisionService(RiskCacheService riskCacheService,
                               ActiveStrategyLoader activeStrategyLoader,
                               RiskContextBuilder riskContextBuilder,
                               DecisionEngine decisionEngine,
                               DecisionResolver decisionResolver,
                               DecisionPersistenceService decisionPersistenceService,
                               RiskCaseService riskCaseService) {
        this.riskCacheService = riskCacheService;
        this.activeStrategyLoader = activeStrategyLoader;
        this.riskContextBuilder = riskContextBuilder;
        this.decisionEngine = decisionEngine;
        this.decisionResolver = decisionResolver;
        this.decisionPersistenceService = decisionPersistenceService;
        this.riskCaseService = riskCaseService;
    }

    @Transactional
    public RiskDecisionResponse decide(RiskDecisionRequest request) {
        RiskDecisionRequest normalized = normalize(request);
        return riskCacheService.getIdempotentDecision(normalized.requestNo(), RiskDecisionResponse.class)
                .map(this::markIdempotent)
                .or(() -> decisionPersistenceService.findResponseByRequestNo(normalized.requestNo()).map(this::withCaseNo))
                .orElseGet(() -> createAndPersistDecision(normalized));
    }

    public RiskDecisionResponse simulate(RiskDecisionRequest request) {
        return evaluate(normalize(request), NoGenerator.simulateNo(), NoGenerator.eventNo(), null, false);
    }

    private RiskDecisionResponse createAndPersistDecision(RiskDecisionRequest request) {
        RiskDecisionResponse response = evaluate(request, NoGenerator.decisionNo(), NoGenerator.eventNo(), null, false);
        decisionPersistenceService.persistDecision(request, response);
        String caseNo = riskCaseService.createCaseIfReview(request, response).orElse(null);
        RiskDecisionResponse finalResponse = withCaseNo(response, caseNo);
        riskCacheService.putIdempotentDecision(request.requestNo(), finalResponse);
        return finalResponse;
    }

    private RiskDecisionResponse evaluate(RiskDecisionRequest request, String decisionNo, String eventNo, String caseNo, boolean idempotent) {
        long start = System.nanoTime();
        ActiveStrategySnapshot strategy = activeStrategyLoader.load(request.eventType());
        RiskContext context = riskContextBuilder.build(request);
        DecisionEngineResult engineResult = decisionEngine.execute(strategy, context);
        DecisionOutcome outcome = decisionResolver.resolve(engineResult.rawScore(), engineResult.hitRules());
        long costMs = Math.max(1, (System.nanoTime() - start) / 1_000_000);

        return new RiskDecisionResponse(
                decisionNo,
                eventNo,
                caseNo,
                outcome.decision(),
                outcome.riskScore(),
                outcome.riskLevel(),
                strategy.strategyId(),
                strategy.strategyVersion(),
                toHitRuleResponses(engineResult.hitRules()),
                outcome.reason(),
                costMs,
                idempotent
        );
    }

    private RiskDecisionRequest normalize(RiskDecisionRequest request) {
        String eventType = EnumUtils.requireName(EventType.class, request.eventType(), "eventType");
        return new RiskDecisionRequest(
                request.requestNo().trim(),
                eventType,
                request.userId().trim(),
                trimToNull(request.deviceId()),
                trimToNull(request.ip()),
                request.amount(),
                trimToNull(request.bizId()),
                trimToNull(request.scene()),
                request.eventTime(),
                request.extra()
        );
    }

    private List<DecisionHitRuleResponse> toHitRuleResponses(List<RuleEvaluationResult> hitRules) {
        return hitRules.stream()
                .map(hit -> new DecisionHitRuleResponse(
                        hit.rule().ruleId(),
                        hit.rule().ruleCode(),
                        hit.rule().ruleName(),
                        hit.rule().ruleVersion(),
                        hit.scoreDelta(),
                        hit.action(),
                        hit.detail()))
                .toList();
    }

    private RiskDecisionResponse markIdempotent(RiskDecisionResponse response) {
        return new RiskDecisionResponse(
                response.decisionNo(),
                response.eventNo(),
                response.caseNo(),
                response.decision(),
                response.riskScore(),
                response.riskLevel(),
                response.strategyId(),
                response.strategyVersion(),
                response.hitRules(),
                response.reason(),
                response.costMs(),
                true
        );
    }

    private RiskDecisionResponse withCaseNo(RiskDecisionResponse response) {
        String caseNo = response.caseNo() == null
                ? riskCaseService.findCaseNoByDecisionNo(response.decisionNo()).orElse(null)
                : response.caseNo();
        return withCaseNo(response, caseNo);
    }

    private RiskDecisionResponse withCaseNo(RiskDecisionResponse response, String caseNo) {
        return new RiskDecisionResponse(
                response.decisionNo(),
                response.eventNo(),
                caseNo,
                response.decision(),
                response.riskScore(),
                response.riskLevel(),
                response.strategyId(),
                response.strategyVersion(),
                response.hitRules(),
                response.reason(),
                response.costMs(),
                response.idempotent()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
