package com.riskguard.decision.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_decision_log")
public class RiskDecisionLog {

    @TableId
    private Long id;
    private String decisionNo;
    private String eventNo;
    private String requestNo;
    private String eventType;
    private String userId;
    private Long strategyId;
    private Integer strategyVersion;
    private Integer riskScore;
    private String riskLevel;
    private String decision;
    private String reason;
    private Integer costMs;
    private String traceId;
    private LocalDateTime createdAt;
}
