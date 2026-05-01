package com.riskguard.strategy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_strategy_rule")
public class RiskStrategyRule {

    @TableId
    private Long id;
    private Long strategyId;
    private Long ruleId;
    private Integer ruleVersion;
    private Integer executeOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
