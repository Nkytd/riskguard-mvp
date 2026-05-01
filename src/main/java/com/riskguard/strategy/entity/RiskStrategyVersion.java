package com.riskguard.strategy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_strategy_version")
public class RiskStrategyVersion {

    @TableId
    private Long id;
    private Long strategyId;
    private Integer version;
    private String ruleSnapshot;
    private Integer grayRatio;
    private String status;
    private String publishNote;
    private Long createdBy;
    private LocalDateTime createdAt;
}
