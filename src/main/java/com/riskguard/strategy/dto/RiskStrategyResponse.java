package com.riskguard.strategy.dto;

import com.riskguard.strategy.entity.RiskStrategy;

import java.time.LocalDateTime;

public record RiskStrategyResponse(
        Long id,
        String strategyCode,
        String strategyName,
        String eventType,
        String status,
        Integer version,
        Integer grayRatio,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RiskStrategyResponse from(RiskStrategy strategy) {
        return new RiskStrategyResponse(
                strategy.getId(),
                strategy.getStrategyCode(),
                strategy.getStrategyName(),
                strategy.getEventType(),
                strategy.getStatus(),
                strategy.getVersion(),
                strategy.getGrayRatio(),
                strategy.getDescription(),
                strategy.getCreatedAt(),
                strategy.getUpdatedAt()
        );
    }
}
