package com.riskguard.decision.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record RiskDecisionRequest(
        @NotBlank String requestNo,
        @NotBlank String eventType,
        @NotBlank String userId,
        String deviceId,
        String ip,
        BigDecimal amount,
        String bizId,
        String scene,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime eventTime,
        Map<String, Object> extra
) {
}
