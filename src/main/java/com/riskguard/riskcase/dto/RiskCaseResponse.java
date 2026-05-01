package com.riskguard.riskcase.dto;

import com.riskguard.riskcase.entity.RiskCase;

import java.time.LocalDateTime;

public record RiskCaseResponse(
        String caseNo,
        String decisionNo,
        String eventNo,
        String userId,
        Integer riskScore,
        String riskLevel,
        String status,
        Long assigneeId,
        String aiSummary,
        String auditResult,
        String auditOpinion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RiskCaseResponse from(RiskCase riskCase) {
        return new RiskCaseResponse(
                riskCase.getCaseNo(),
                riskCase.getDecisionNo(),
                riskCase.getEventNo(),
                riskCase.getUserId(),
                riskCase.getRiskScore(),
                riskCase.getRiskLevel(),
                riskCase.getStatus(),
                riskCase.getAssigneeId(),
                riskCase.getAiSummary(),
                riskCase.getAuditResult(),
                riskCase.getAuditOpinion(),
                riskCase.getCreatedAt(),
                riskCase.getUpdatedAt()
        );
    }
}
