package com.riskguard.profile.dto;

import com.riskguard.profile.entity.RiskIpProfile;

import java.time.LocalDateTime;

public record IpProfileResponse(
        Long id,
        String ip,
        Integer loginCount10m,
        Integer failedLoginCount10m,
        Integer accountCount24h,
        Integer paymentCount24h,
        String location,
        LocalDateTime updatedAt
) {

    public static IpProfileResponse from(RiskIpProfile profile) {
        return new IpProfileResponse(
                profile.getId(),
                profile.getIp(),
                profile.getLoginCount10m(),
                profile.getFailedLoginCount10m(),
                profile.getAccountCount24h(),
                profile.getPaymentCount24h(),
                profile.getLocation(),
                profile.getUpdatedAt()
        );
    }
}
