package com.riskguard.profile.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.riskguard.common.enums.EventType;
import com.riskguard.common.enums.RiskDecision;
import com.riskguard.common.utils.ClockUtils;
import com.riskguard.mq.event.RiskDecisionCreatedEvent;
import com.riskguard.profile.entity.RiskDeviceProfile;
import com.riskguard.profile.entity.RiskIpProfile;
import com.riskguard.profile.entity.RiskUserProfile;
import com.riskguard.profile.mapper.RiskDeviceProfileMapper;
import com.riskguard.profile.mapper.RiskIpProfileMapper;
import com.riskguard.profile.mapper.RiskUserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ProfileAsyncUpdateService {

    private final RiskUserProfileMapper userProfileMapper;
    private final RiskDeviceProfileMapper deviceProfileMapper;
    private final RiskIpProfileMapper ipProfileMapper;

    public ProfileAsyncUpdateService(RiskUserProfileMapper userProfileMapper,
                                     RiskDeviceProfileMapper deviceProfileMapper,
                                     RiskIpProfileMapper ipProfileMapper) {
        this.userProfileMapper = userProfileMapper;
        this.deviceProfileMapper = deviceProfileMapper;
        this.ipProfileMapper = ipProfileMapper;
    }

    @Transactional
    public void applyDecisionEvent(RiskDecisionCreatedEvent event) {
        updateUserProfile(event);
        updateDeviceProfile(event);
        updateIpProfile(event);
    }

    private void updateUserProfile(RiskDecisionCreatedEvent event) {
        if (!StringUtils.hasText(event.userId())) {
            return;
        }
        RiskUserProfile profile = userProfileMapper.selectOne(Wrappers.<RiskUserProfile>lambdaQuery()
                .eq(RiskUserProfile::getUserId, event.userId()));
        if (profile == null) {
            profile = new RiskUserProfile();
            profile.setUserId(event.userId());
            profile.setRegisterTime(event.eventTime());
            profile.setAccountLevel("NORMAL");
            profile.setRealNameVerified(false);
            profile.setTotalPayAmount(BigDecimal.ZERO);
            profile.setPayCount24h(0);
            profile.setFailedLoginCount10m(0);
            applyUserDelta(profile, event);
            userProfileMapper.insert(profile);
            return;
        }
        applyUserDelta(profile, event);
        userProfileMapper.updateById(profile);
    }

    private void applyUserDelta(RiskUserProfile profile, RiskDecisionCreatedEvent event) {
        if (EventType.PAYMENT.name().equals(event.eventType())) {
            profile.setPayCount24h(increment(profile.getPayCount24h()));
            profile.setTotalPayAmount(safeAmount(profile.getTotalPayAmount()).add(safeAmount(event.amount())));
        }
        if (EventType.LOGIN.name().equals(event.eventType())) {
            profile.setLastLoginIp(event.ip());
            profile.setLastLoginTime(event.eventTime());
            if (RiskDecision.REJECT.name().equals(event.decision())) {
                profile.setFailedLoginCount10m(increment(profile.getFailedLoginCount10m()));
            }
        }
        profile.setUpdatedAt(ClockUtils.now());
    }

    private void updateDeviceProfile(RiskDecisionCreatedEvent event) {
        if (!StringUtils.hasText(event.deviceId())) {
            return;
        }
        RiskDeviceProfile profile = deviceProfileMapper.selectOne(Wrappers.<RiskDeviceProfile>lambdaQuery()
                .eq(RiskDeviceProfile::getDeviceId, event.deviceId()));
        if (profile == null) {
            profile = new RiskDeviceProfile();
            profile.setDeviceId(event.deviceId());
            profile.setAccountCount24h(1);
            profile.setAccountCountTotal(1);
            profile.setPaymentCount24h(0);
            profile.setFirstSeenTime(event.eventTime());
            applyDeviceDelta(profile, event);
            deviceProfileMapper.insert(profile);
            return;
        }
        applyDeviceDelta(profile, event);
        deviceProfileMapper.updateById(profile);
    }

    private void applyDeviceDelta(RiskDeviceProfile profile, RiskDecisionCreatedEvent event) {
        if (EventType.PAYMENT.name().equals(event.eventType())) {
            profile.setPaymentCount24h(increment(profile.getPaymentCount24h()));
        }
        profile.setLastSeenTime(event.eventTime());
        profile.setUpdatedAt(ClockUtils.now());
    }

    private void updateIpProfile(RiskDecisionCreatedEvent event) {
        if (!StringUtils.hasText(event.ip())) {
            return;
        }
        RiskIpProfile profile = ipProfileMapper.selectOne(Wrappers.<RiskIpProfile>lambdaQuery()
                .eq(RiskIpProfile::getIp, event.ip()));
        if (profile == null) {
            profile = new RiskIpProfile();
            profile.setIp(event.ip());
            profile.setLoginCount10m(0);
            profile.setFailedLoginCount10m(0);
            profile.setAccountCount24h(1);
            profile.setPaymentCount24h(0);
            profile.setLocation("unknown");
            applyIpDelta(profile, event);
            ipProfileMapper.insert(profile);
            return;
        }
        applyIpDelta(profile, event);
        ipProfileMapper.updateById(profile);
    }

    private void applyIpDelta(RiskIpProfile profile, RiskDecisionCreatedEvent event) {
        if (EventType.LOGIN.name().equals(event.eventType())) {
            profile.setLoginCount10m(increment(profile.getLoginCount10m()));
            if (RiskDecision.REJECT.name().equals(event.decision())) {
                profile.setFailedLoginCount10m(increment(profile.getFailedLoginCount10m()));
            }
        }
        if (EventType.PAYMENT.name().equals(event.eventType())) {
            profile.setPaymentCount24h(increment(profile.getPaymentCount24h()));
        }
        profile.setUpdatedAt(ClockUtils.now());
    }

    private Integer increment(Integer value) {
        return value == null ? 1 : value + 1;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
