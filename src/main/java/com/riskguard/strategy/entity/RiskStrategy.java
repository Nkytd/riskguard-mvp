package com.riskguard.strategy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_strategy")
public class RiskStrategy {

    @TableId
    private Long id;
    private String strategyCode;
    private String strategyName;
    private String eventType;
    private String status;
    private Integer version;
    private Integer grayRatio;
    private String description;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
