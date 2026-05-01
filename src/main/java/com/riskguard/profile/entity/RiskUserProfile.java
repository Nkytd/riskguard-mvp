package com.riskguard.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_user_profile")
public class RiskUserProfile {

    @TableId
    private Long id;
    private String userId;
    private LocalDateTime registerTime;
    private String accountLevel;
    private Boolean realNameVerified;
    private BigDecimal totalPayAmount;
    @TableField("pay_count_24h")
    private Integer payCount24h;
    @TableField("failed_login_count_10m")
    private Integer failedLoginCount10m;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private LocalDateTime updatedAt;
}
