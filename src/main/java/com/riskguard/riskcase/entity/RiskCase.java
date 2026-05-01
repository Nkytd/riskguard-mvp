package com.riskguard.riskcase.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_case")
public class RiskCase {

    @TableId
    private Long id;
    private String caseNo;
    private String decisionNo;
    private String eventNo;
    private String userId;
    private Integer riskScore;
    private String riskLevel;
    private String status;
    private Long assigneeId;
    private String aiSummary;
    private String auditResult;
    private String auditOpinion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
