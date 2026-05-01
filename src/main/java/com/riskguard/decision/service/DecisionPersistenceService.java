package com.riskguard.decision.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.api.PageResponse;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.common.trace.TraceIdHolder;
import com.riskguard.common.utils.NoGenerator;
import com.riskguard.decision.dto.DecisionHitRuleResponse;
import com.riskguard.decision.dto.DecisionLogResponse;
import com.riskguard.decision.dto.RiskDecisionRequest;
import com.riskguard.decision.dto.RiskDecisionResponse;
import com.riskguard.decision.entity.RiskDecisionHitRule;
import com.riskguard.decision.entity.RiskDecisionLog;
import com.riskguard.decision.entity.RiskEvent;
import com.riskguard.decision.mapper.RiskDecisionHitRuleMapper;
import com.riskguard.decision.mapper.RiskDecisionLogMapper;
import com.riskguard.decision.mapper.RiskEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class DecisionPersistenceService {

    private final RiskEventMapper riskEventMapper;
    private final RiskDecisionLogMapper decisionLogMapper;
    private final RiskDecisionHitRuleMapper hitRuleMapper;
    private final ObjectMapper objectMapper;

    public DecisionPersistenceService(RiskEventMapper riskEventMapper,
                                      RiskDecisionLogMapper decisionLogMapper,
                                      RiskDecisionHitRuleMapper hitRuleMapper,
                                      ObjectMapper objectMapper) {
        this.riskEventMapper = riskEventMapper;
        this.decisionLogMapper = decisionLogMapper;
        this.hitRuleMapper = hitRuleMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RiskEvent persistDecision(RiskDecisionRequest request, RiskDecisionResponse response) {
        RiskEvent event = buildEvent(request, response.eventNo());
        riskEventMapper.insert(event);

        RiskDecisionLog log = new RiskDecisionLog();
        log.setDecisionNo(response.decisionNo());
        log.setEventNo(response.eventNo());
        log.setRequestNo(request.requestNo());
        log.setEventType(request.eventType());
        log.setUserId(request.userId());
        log.setStrategyId(response.strategyId());
        log.setStrategyVersion(response.strategyVersion());
        log.setRiskScore(response.riskScore());
        log.setRiskLevel(response.riskLevel());
        log.setDecision(response.decision());
        log.setReason(response.reason());
        log.setCostMs(response.costMs() == null ? 0 : response.costMs().intValue());
        log.setTraceId(TraceIdHolder.getTraceId());
        decisionLogMapper.insert(log);

        for (DecisionHitRuleResponse hitRule : response.hitRules()) {
            RiskDecisionHitRule entity = new RiskDecisionHitRule();
            entity.setDecisionNo(response.decisionNo());
            entity.setRuleId(hitRule.ruleId());
            entity.setRuleCode(hitRule.ruleCode());
            entity.setRuleName(hitRule.ruleName());
            entity.setRuleVersion(hitRule.ruleVersion());
            entity.setScoreDelta(hitRule.scoreDelta());
            entity.setAction(hitRule.action());
            entity.setHitDetail(hitRule.hitDetail());
            hitRuleMapper.insert(entity);
        }
        return event;
    }

    public Optional<RiskDecisionResponse> findResponseByRequestNo(String requestNo) {
        RiskDecisionLog log = decisionLogMapper.selectOne(Wrappers.<RiskDecisionLog>lambdaQuery()
                .eq(RiskDecisionLog::getRequestNo, requestNo));
        if (log == null) {
            return Optional.empty();
        }
        return Optional.of(toDecisionResponse(log, true));
    }

    public DecisionLogResponse getDecisionLog(String decisionNo) {
        RiskDecisionLog log = requireDecisionLog(decisionNo);
        return DecisionLogResponse.from(log, listHitRules(decisionNo));
    }

    public List<DecisionHitRuleResponse> listHitRules(String decisionNo) {
        return hitRuleMapper.selectList(Wrappers.<RiskDecisionHitRule>lambdaQuery()
                        .eq(RiskDecisionHitRule::getDecisionNo, decisionNo)
                        .orderByAsc(RiskDecisionHitRule::getId))
                .stream()
                .map(this::toHitRuleResponse)
                .toList();
    }

    public PageResponse<DecisionLogResponse> pageDecisionLogs(long pageNo, long pageSize, String eventType,
                                                              String decision, String userId) {
        var query = Wrappers.<RiskDecisionLog>lambdaQuery()
                .eq(StringUtils.hasText(eventType), RiskDecisionLog::getEventType, eventType)
                .eq(StringUtils.hasText(decision), RiskDecisionLog::getDecision, decision)
                .eq(StringUtils.hasText(userId), RiskDecisionLog::getUserId, userId)
                .orderByDesc(RiskDecisionLog::getCreatedAt)
                .orderByDesc(RiskDecisionLog::getId);
        Page<RiskDecisionLog> page = decisionLogMapper.selectPage(Page.of(pageNo, pageSize), query);
        return PageResponse.of(page.getRecords().stream()
                        .map(log -> DecisionLogResponse.from(log, List.of()))
                        .toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    private RiskDecisionLog requireDecisionLog(String decisionNo) {
        RiskDecisionLog log = decisionLogMapper.selectOne(Wrappers.<RiskDecisionLog>lambdaQuery()
                .eq(RiskDecisionLog::getDecisionNo, decisionNo));
        if (log == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Decision log not found: " + decisionNo);
        }
        return log;
    }

    private RiskDecisionResponse toDecisionResponse(RiskDecisionLog log, boolean idempotent) {
        return new RiskDecisionResponse(
                log.getDecisionNo(),
                log.getEventNo(),
                null,
                log.getDecision(),
                log.getRiskScore(),
                log.getRiskLevel(),
                log.getStrategyId(),
                log.getStrategyVersion(),
                listHitRules(log.getDecisionNo()),
                log.getReason(),
                log.getCostMs() == null ? 0L : log.getCostMs().longValue(),
                idempotent
        );
    }

    private DecisionHitRuleResponse toHitRuleResponse(RiskDecisionHitRule hitRule) {
        return new DecisionHitRuleResponse(
                hitRule.getRuleId(),
                hitRule.getRuleCode(),
                hitRule.getRuleName(),
                hitRule.getRuleVersion(),
                hitRule.getScoreDelta(),
                hitRule.getAction(),
                hitRule.getHitDetail()
        );
    }

    private RiskEvent buildEvent(RiskDecisionRequest request, String eventNo) {
        RiskEvent event = new RiskEvent();
        event.setEventNo(eventNo == null ? NoGenerator.eventNo() : eventNo);
        event.setRequestNo(request.requestNo());
        event.setEventType(request.eventType());
        event.setUserId(request.userId());
        event.setDeviceId(request.deviceId());
        event.setIp(request.ip());
        event.setAmount(request.amount());
        event.setBizId(request.bizId());
        event.setScene(request.scene());
        event.setEventTime(request.eventTime());
        event.setRawPayload(toJson(request));
        return event;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize risk event payload");
        }
    }
}
