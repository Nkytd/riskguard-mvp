package com.riskguard.profile.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.profile.dto.DeviceProfileResponse;
import com.riskguard.profile.dto.IpProfileResponse;
import com.riskguard.profile.dto.UserProfileResponse;
import com.riskguard.profile.entity.RiskDeviceProfile;
import com.riskguard.profile.entity.RiskIpProfile;
import com.riskguard.profile.entity.RiskUserProfile;
import com.riskguard.profile.mapper.RiskDeviceProfileMapper;
import com.riskguard.profile.mapper.RiskIpProfileMapper;
import com.riskguard.profile.mapper.RiskUserProfileMapper;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final RiskUserProfileMapper userProfileMapper;
    private final RiskDeviceProfileMapper deviceProfileMapper;
    private final RiskIpProfileMapper ipProfileMapper;

    public ProfileService(RiskUserProfileMapper userProfileMapper,
                          RiskDeviceProfileMapper deviceProfileMapper,
                          RiskIpProfileMapper ipProfileMapper) {
        this.userProfileMapper = userProfileMapper;
        this.deviceProfileMapper = deviceProfileMapper;
        this.ipProfileMapper = ipProfileMapper;
    }

    public UserProfileResponse getUserProfile(String userId) {
        RiskUserProfile profile = userProfileMapper.selectOne(Wrappers.<RiskUserProfile>lambdaQuery()
                .eq(RiskUserProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User profile not found: " + userId);
        }
        return UserProfileResponse.from(profile);
    }

    public DeviceProfileResponse getDeviceProfile(String deviceId) {
        RiskDeviceProfile profile = deviceProfileMapper.selectOne(Wrappers.<RiskDeviceProfile>lambdaQuery()
                .eq(RiskDeviceProfile::getDeviceId, deviceId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Device profile not found: " + deviceId);
        }
        return DeviceProfileResponse.from(profile);
    }

    public IpProfileResponse getIpProfile(String ip) {
        RiskIpProfile profile = ipProfileMapper.selectOne(Wrappers.<RiskIpProfile>lambdaQuery()
                .eq(RiskIpProfile::getIp, ip));
        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IP profile not found: " + ip);
        }
        return IpProfileResponse.from(profile);
    }
}
