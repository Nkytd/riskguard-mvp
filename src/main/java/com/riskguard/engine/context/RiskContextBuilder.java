package com.riskguard.engine.context;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.riskguard.common.enums.ListType;
import com.riskguard.common.enums.ObjectType;
import com.riskguard.common.enums.PublishStatus;
import com.riskguard.decision.dto.RiskDecisionRequest;
import com.riskguard.profile.entity.RiskDeviceProfile;
import com.riskguard.profile.entity.RiskIpProfile;
import com.riskguard.profile.entity.RiskUserProfile;
import com.riskguard.profile.mapper.RiskDeviceProfileMapper;
import com.riskguard.profile.mapper.RiskIpProfileMapper;
import com.riskguard.profile.mapper.RiskUserProfileMapper;
import com.riskguard.risklist.entity.RiskList;
import com.riskguard.risklist.mapper.RiskListMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class RiskContextBuilder {

    private final RiskUserProfileMapper userProfileMapper;
    private final RiskDeviceProfileMapper deviceProfileMapper;
    private final RiskIpProfileMapper ipProfileMapper;
    private final RiskListMapper riskListMapper;

    public RiskContextBuilder(RiskUserProfileMapper userProfileMapper,
                              RiskDeviceProfileMapper deviceProfileMapper,
                              RiskIpProfileMapper ipProfileMapper,
                              RiskListMapper riskListMapper) {
        this.userProfileMapper = userProfileMapper;
        this.deviceProfileMapper = deviceProfileMapper;
        this.ipProfileMapper = ipProfileMapper;
        this.riskListMapper = riskListMapper;
    }

    public RiskContext build(RiskDecisionRequest request) {
        RiskUserProfile user = loadUserProfile(request.userId());
        RiskDeviceProfile device = loadDeviceProfile(request.deviceId());
        RiskIpProfile ip = loadIpProfile(request.ip());

        Map<String, Object> variables = new HashMap<>();
        variables.put("event", buildEvent(request));
        variables.put("user", buildUser(user));
        variables.put("device", buildDevice(device));
        variables.put("ip", buildIp(ip));
        variables.put("list", buildList(request));
        return new RiskContext(variables);
    }

    private Map<String, Object> buildEvent(RiskDecisionRequest request) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", request.eventType());
        event.put("userId", request.userId());
        event.put("deviceId", request.deviceId());
        event.put("ip", request.ip());
        event.put("amount", toDouble(request.amount()));
        event.put("bizId", request.bizId());
        event.put("scene", request.scene());
        event.put("eventTime", request.eventTime() == null ? null : request.eventTime().toString());
        event.put("extra", request.extra() == null ? Map.of() : request.extra());
        return event;
    }

    private Map<String, Object> buildUser(RiskUserProfile profile) {
        Map<String, Object> user = new HashMap<>();
        user.put("registerDays", profile == null ? 9999 : registerDays(profile.getRegisterTime()));
        user.put("accountLevel", profile == null ? null : profile.getAccountLevel());
        user.put("realNameVerified", profile != null && Boolean.TRUE.equals(profile.getRealNameVerified()));
        user.put("totalPayAmount", profile == null ? 0D : toDouble(profile.getTotalPayAmount()));
        user.put("payCount24h", profile == null ? 0 : nullToZero(profile.getPayCount24h()));
        user.put("failedLoginCount10m", profile == null ? 0 : nullToZero(profile.getFailedLoginCount10m()));
        user.put("lastLoginIp", profile == null ? null : profile.getLastLoginIp());
        user.put("lastLoginTime", profile == null || profile.getLastLoginTime() == null ? null : profile.getLastLoginTime().toString());
        return user;
    }

    private Map<String, Object> buildDevice(RiskDeviceProfile profile) {
        Map<String, Object> device = new HashMap<>();
        device.put("accountCount24h", profile == null ? 0 : nullToZero(profile.getAccountCount24h()));
        device.put("accountCountTotal", profile == null ? 0 : nullToZero(profile.getAccountCountTotal()));
        device.put("paymentCount24h", profile == null ? 0 : nullToZero(profile.getPaymentCount24h()));
        device.put("firstSeenTime", profile == null || profile.getFirstSeenTime() == null ? null : profile.getFirstSeenTime().toString());
        device.put("lastSeenTime", profile == null || profile.getLastSeenTime() == null ? null : profile.getLastSeenTime().toString());
        return device;
    }

    private Map<String, Object> buildIp(RiskIpProfile profile) {
        Map<String, Object> ip = new HashMap<>();
        ip.put("loginCount10m", profile == null ? 0 : nullToZero(profile.getLoginCount10m()));
        ip.put("failedLoginCount10m", profile == null ? 0 : nullToZero(profile.getFailedLoginCount10m()));
        ip.put("accountCount24h", profile == null ? 0 : nullToZero(profile.getAccountCount24h()));
        ip.put("paymentCount24h", profile == null ? 0 : nullToZero(profile.getPaymentCount24h()));
        ip.put("location", profile == null ? null : profile.getLocation());
        return ip;
    }

    private Map<String, Object> buildList(RiskDecisionRequest request) {
        Map<String, Object> list = new HashMap<>();
        list.put("userBlacklistHit", hasActiveList(ListType.BLACK, ObjectType.USER, request.userId()));
        list.put("userWhitelistHit", hasActiveList(ListType.WHITE, ObjectType.USER, request.userId()));
        list.put("deviceBlacklistHit", hasActiveList(ListType.BLACK, ObjectType.DEVICE, request.deviceId()));
        list.put("deviceWhitelistHit", hasActiveList(ListType.WHITE, ObjectType.DEVICE, request.deviceId()));
        list.put("ipBlacklistHit", hasActiveList(ListType.BLACK, ObjectType.IP, request.ip()));
        list.put("ipWhitelistHit", hasActiveList(ListType.WHITE, ObjectType.IP, request.ip()));
        return list;
    }

    private boolean hasActiveList(ListType listType, ObjectType objectType, String objectValue) {
        if (!StringUtils.hasText(objectValue)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Long count = riskListMapper.selectCount(Wrappers.<RiskList>lambdaQuery()
                .eq(RiskList::getListType, listType.name())
                .eq(RiskList::getObjectType, objectType.name())
                .eq(RiskList::getObjectValue, objectValue)
                .eq(RiskList::getStatus, PublishStatus.ENABLED.name())
                .and(wrapper -> wrapper.isNull(RiskList::getStartTime).or().le(RiskList::getStartTime, now))
                .and(wrapper -> wrapper.isNull(RiskList::getEndTime).or().gt(RiskList::getEndTime, now)));
        return count != null && count > 0;
    }

    private RiskUserProfile loadUserProfile(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return userProfileMapper.selectOne(Wrappers.<RiskUserProfile>lambdaQuery()
                .eq(RiskUserProfile::getUserId, userId));
    }

    private RiskDeviceProfile loadDeviceProfile(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return deviceProfileMapper.selectOne(Wrappers.<RiskDeviceProfile>lambdaQuery()
                .eq(RiskDeviceProfile::getDeviceId, deviceId));
    }

    private RiskIpProfile loadIpProfile(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        return ipProfileMapper.selectOne(Wrappers.<RiskIpProfile>lambdaQuery()
                .eq(RiskIpProfile::getIp, ip));
    }

    private int registerDays(LocalDateTime registerTime) {
        if (registerTime == null) {
            return 9999;
        }
        return Math.max(0, (int) ChronoUnit.DAYS.between(registerTime, LocalDateTime.now()));
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }
}
