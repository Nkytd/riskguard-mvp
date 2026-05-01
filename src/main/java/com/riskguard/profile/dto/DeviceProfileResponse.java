package com.riskguard.profile.dto;

import com.riskguard.profile.entity.RiskDeviceProfile;

import java.time.LocalDateTime;

public record DeviceProfileResponse(
        Long id,
        String deviceId,
        Integer accountCount24h,
        Integer accountCountTotal,
        Integer paymentCount24h,
        LocalDateTime firstSeenTime,
        LocalDateTime lastSeenTime,
        LocalDateTime updatedAt
) {

    public static DeviceProfileResponse from(RiskDeviceProfile profile) {
        return new DeviceProfileResponse(
                profile.getId(),
                profile.getDeviceId(),
                profile.getAccountCount24h(),
                profile.getAccountCountTotal(),
                profile.getPaymentCount24h(),
                profile.getFirstSeenTime(),
                profile.getLastSeenTime(),
                profile.getUpdatedAt()
        );
    }
}
