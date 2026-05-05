package com.riskguard.mq.event;

import java.time.LocalDateTime;

public record RiskCaseCreatedEvent(
        String caseNo,
        String decisionNo,
        String eventNo,
        String userId,
        Integer riskScore,
        String riskLevel,
        String status,
        LocalDateTime occurredAt
) {
}
