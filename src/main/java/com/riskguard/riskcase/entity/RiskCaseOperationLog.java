package com.riskguard.riskcase.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_case_operation_log")
public class RiskCaseOperationLog {

    @TableId
    private Long id;
    private String caseNo;
    private Long operatorId;
    private String operation;
    private String beforeStatus;
    private String afterStatus;
    private String remark;
    private LocalDateTime createdAt;
}
