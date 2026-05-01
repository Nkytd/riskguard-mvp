package com.riskguard.risklist.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_list")
public class RiskList {

    @TableId
    private Long id;
    private String listType;
    private String objectType;
    private String objectValue;
    private String riskLevel;
    private String effectType;
    private Integer scoreDelta;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
