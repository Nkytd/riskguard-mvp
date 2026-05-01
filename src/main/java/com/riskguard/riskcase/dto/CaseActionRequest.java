package com.riskguard.riskcase.dto;

public record CaseActionRequest(
        Long operatorId,
        Long assigneeId,
        String auditOpinion,
        String remark
) {
}
