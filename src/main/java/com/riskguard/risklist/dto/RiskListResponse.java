package com.riskguard.risklist.dto;

import com.riskguard.risklist.entity.RiskList;

import java.time.LocalDateTime;

public record RiskListResponse(
        Long id,
        String listType,
        String objectType,
        String objectValue,
        String riskLevel,
        String effectType,
        Integer scoreDelta,
        String reason,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RiskListResponse from(RiskList riskList) {
        return new RiskListResponse(
                riskList.getId(),
                riskList.getListType(),
                riskList.getObjectType(),
                riskList.getObjectValue(),
                riskList.getRiskLevel(),
                riskList.getEffectType(),
                riskList.getScoreDelta(),
                riskList.getReason(),
                riskList.getStartTime(),
                riskList.getEndTime(),
                riskList.getStatus(),
                riskList.getCreatedAt(),
                riskList.getUpdatedAt()
        );
    }
}
