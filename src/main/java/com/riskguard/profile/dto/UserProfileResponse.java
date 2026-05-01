package com.riskguard.profile.dto;

import com.riskguard.profile.entity.RiskUserProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String userId,
        LocalDateTime registerTime,
        String accountLevel,
        Boolean realNameVerified,
        BigDecimal totalPayAmount,
        Integer payCount24h,
        Integer failedLoginCount10m,
        String lastLoginIp,
        LocalDateTime lastLoginTime,
        LocalDateTime updatedAt
) {

    public static UserProfileResponse from(RiskUserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getRegisterTime(),
                profile.getAccountLevel(),
                profile.getRealNameVerified(),
                profile.getTotalPayAmount(),
                profile.getPayCount24h(),
                profile.getFailedLoginCount10m(),
                profile.getLastLoginIp(),
                profile.getLastLoginTime(),
                profile.getUpdatedAt()
        );
    }
}
