package com.riskguard.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_ip_profile")
public class RiskIpProfile {

    @TableId
    private Long id;
    private String ip;
    @TableField("login_count_10m")
    private Integer loginCount10m;
    @TableField("failed_login_count_10m")
    private Integer failedLoginCount10m;
    @TableField("account_count_24h")
    private Integer accountCount24h;
    @TableField("payment_count_24h")
    private Integer paymentCount24h;
    private String location;
    private LocalDateTime updatedAt;
}
