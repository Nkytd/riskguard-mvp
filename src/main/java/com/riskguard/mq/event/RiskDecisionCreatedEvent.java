package com.riskguard.mq.event;

import com.riskguard.decision.dto.RiskDecisionRequest;
import com.riskguard.decision.dto.RiskDecisionResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskDecisionCreatedEvent(
        String requestNo,
        String decisionNo,
        String eventNo,
        String eventType,
        String userId,
        String deviceId,
        String ip,
        BigDecimal amount,
        String bizId,
        String scene,
        LocalDateTime eventTime,
        String decision,
        Integer riskScore,
        String riskLevel,
        Long strategyId,
        Integer strategyVersion,
        String caseNo,
        LocalDateTime occurredAt
) {

    public static RiskDecisionCreatedEvent from(RiskDecisionRequest request, RiskDecisionResponse response, LocalDateTime occurredAt) {
        return new RiskDecisionCreatedEvent(
                request.requestNo(),
                response.decisionNo(),
                response.eventNo(),
                request.eventType(),
                request.userId(),
                request.deviceId(),
                request.ip(),
                request.amount(),
                request.bizId(),
                request.scene(),
                request.eventTime(),
                response.decision(),
                response.riskScore(),
                response.riskLevel(),
                response.strategyId(),
                response.strategyVersion(),
                response.caseNo(),
                occurredAt
        );
    }
}
