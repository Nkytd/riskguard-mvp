package com.riskguard.decision.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_decision_hit_rule")
public class RiskDecisionHitRule {

    @TableId
    private Long id;
    private String decisionNo;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private Integer ruleVersion;
    private Integer scoreDelta;
    private String action;
    private String hitDetail;
    private LocalDateTime createdAt;
}
