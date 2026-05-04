package com.riskguard.dashboard.dto;

import java.time.LocalDateTime;

public record RuleHitRankItem(
        String ruleCode,
        String ruleName,
        long hitCount,
        long totalScoreDelta,
        LocalDateTime lastHitAt
) {
}
