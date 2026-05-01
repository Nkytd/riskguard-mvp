package com.riskguard.rule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_rule_version")
public class RiskRuleVersion {

    @TableId
    private Long id;
    private Long ruleId;
    private Integer version;
    private String expressionText;
    private Integer score;
    private String action;
    private Integer priority;
    private String status;
    private String publishNote;
    private Long createdBy;
    private LocalDateTime createdAt;
}
