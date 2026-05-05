package com.riskguard.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RuleHitRankRow {

    private String ruleCode;
    private String ruleName;
    private Long hitCount;
    private Long totalScoreDelta;
    private LocalDateTime lastHitAt;
}
