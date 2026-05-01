package com.riskguard.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("risk_device_profile")
public class RiskDeviceProfile {

    @TableId
    private Long id;
    private String deviceId;
    @TableField("account_count_24h")
    private Integer accountCount24h;
    private Integer accountCountTotal;
    @TableField("payment_count_24h")
    private Integer paymentCount24h;
    private LocalDateTime firstSeenTime;
    private LocalDateTime lastSeenTime;
    private LocalDateTime updatedAt;
}
