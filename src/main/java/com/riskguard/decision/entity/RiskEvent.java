package com.riskguard.decision.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_event")
public class RiskEvent {

    @TableId
    private Long id;
    private String eventNo;
    private String requestNo;
    private String eventType;
    private String userId;
    private String deviceId;
    private String ip;
    private BigDecimal amount;
    private String bizId;
    private String scene;
    private LocalDateTime eventTime;
    private String rawPayload;
    private LocalDateTime createdAt;
}
