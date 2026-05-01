package com.riskguard.strategy.dto;

import com.riskguard.strategy.entity.RiskStrategyVersion;

import java.time.LocalDateTime;

public record StrategyVersionResponse(
        Long id,
        Long strategyId,
        Integer version,
        String ruleSnapshot,
        Integer grayRatio,
        String status,
        String publishNote,
        LocalDateTime createdAt
) {

    public static StrategyVersionResponse from(RiskStrategyVersion version) {
        return new StrategyVersionResponse(
                version.getId(),
                version.getStrategyId(),
                version.getVersion(),
                version.getRuleSnapshot(),
                version.getGrayRatio(),
                version.getStatus(),
                version.getPublishNote(),
                version.getCreatedAt()
        );
    }
}
