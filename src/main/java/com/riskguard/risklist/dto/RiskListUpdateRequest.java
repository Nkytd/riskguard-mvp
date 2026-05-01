package com.riskguard.risklist.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record RiskListUpdateRequest(
        String riskLevel,
        @NotBlank String effectType,
        Integer scoreDelta,
        String reason,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
