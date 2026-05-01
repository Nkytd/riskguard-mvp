package com.riskguard.rule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_rule")
public class RiskRule {

    @TableId
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String eventType;
    private String expressionText;
    private Integer score;
    private String action;
    private Integer priority;
    private String status;
    private Integer version;
    private String description;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
