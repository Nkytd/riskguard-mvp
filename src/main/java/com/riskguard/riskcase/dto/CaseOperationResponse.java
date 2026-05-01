package com.riskguard.riskcase.dto;

import com.riskguard.riskcase.entity.RiskCaseOperationLog;

import java.time.LocalDateTime;

public record CaseOperationResponse(
        String caseNo,
        Long operatorId,
        String operation,
        String beforeStatus,
        String afterStatus,
        String remark,
        LocalDateTime createdAt
) {

    public static CaseOperationResponse from(RiskCaseOperationLog log) {
        return new CaseOperationResponse(
                log.getCaseNo(),
                log.getOperatorId(),
                log.getOperation(),
                log.getBeforeStatus(),
                log.getAfterStatus(),
                log.getRemark(),
                log.getCreatedAt()
        );
    }
}
