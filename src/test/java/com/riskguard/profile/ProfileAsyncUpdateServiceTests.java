package com.riskguard.profile;

import com.riskguard.mq.event.RiskDecisionCreatedEvent;
import com.riskguard.profile.entity.RiskDeviceProfile;
import com.riskguard.profile.entity.RiskIpProfile;
import com.riskguard.profile.entity.RiskUserProfile;
import com.riskguard.profile.mapper.RiskDeviceProfileMapper;
import com.riskguard.profile.mapper.RiskIpProfileMapper;
import com.riskguard.profile.mapper.RiskUserProfileMapper;
import com.riskguard.profile.service.ProfileAsyncUpdateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAsyncUpdateServiceTests {

    @Mock
    private RiskUserProfileMapper userProfileMapper;
    @Mock
    private RiskDeviceProfileMapper deviceProfileMapper;
    @Mock
    private RiskIpProfileMapper ipProfileMapper;

    @Test
    void paymentDecisionUpdatesExistingProfiles() {
        ProfileAsyncUpdateService service = new ProfileAsyncUpdateService(userProfileMapper, deviceProfileMapper, ipProfileMapper);
        RiskUserProfile userProfile = new RiskUserProfile();
        userProfile.setId(1L);
        userProfile.setUserId("U10001");
        userProfile.setTotalPayAmount(new BigDecimal("100.00"));
        userProfile.setPayCount24h(2);
        userProfile.setFailedLoginCount10m(0);
        RiskDeviceProfile deviceProfile = new RiskDeviceProfile();
        deviceProfile.setId(2L);
        deviceProfile.setDeviceId("D10001");
        deviceProfile.setPaymentCount24h(3);
        RiskIpProfile ipProfile = new RiskIpProfile();
        ipProfile.setId(3L);
        ipProfile.setIp("127.0.0.1");
        ipProfile.setPaymentCount24h(4);

        when(userProfileMapper.selectOne(any())).thenReturn(userProfile);
        when(deviceProfileMapper.selectOne(any())).thenReturn(deviceProfile);
        when(ipProfileMapper.selectOne(any())).thenReturn(ipProfile);

        service.applyDecisionEvent(paymentEvent("U10001", "D10001", "127.0.0.1"));

        assertThat(userProfile.getPayCount24h()).isEqualTo(3);
        assertThat(userProfile.getTotalPayAmount()).isEqualByComparingTo("229.90");
        assertThat(deviceProfile.getPaymentCount24h()).isEqualTo(4);
        assertThat(ipProfile.getPaymentCount24h()).isEqualTo(5);
        verify(userProfileMapper).updateById(userProfile);
        verify(deviceProfileMapper).updateById(deviceProfile);
        verify(ipProfileMapper).updateById(ipProfile);
    }

    @Test
    void paymentDecisionCreatesMissingUserProfile() {
        ProfileAsyncUpdateService service = new ProfileAsyncUpdateService(userProfileMapper, deviceProfileMapper, ipProfileMapper);
        when(userProfileMapper.selectOne(any())).thenReturn(null);

        service.applyDecisionEvent(paymentEvent("U-NEW", null, null));

        ArgumentCaptor<RiskUserProfile> captor = ArgumentCaptor.forClass(RiskUserProfile.class);
        verify(userProfileMapper).insert(captor.capture());
        RiskUserProfile profile = captor.getValue();
        assertThat(profile.getUserId()).isEqualTo("U-NEW");
        assertThat(profile.getAccountLevel()).isEqualTo("NORMAL");
        assertThat(profile.getRealNameVerified()).isFalse();
        assertThat(profile.getPayCount24h()).isEqualTo(1);
        assertThat(profile.getTotalPayAmount()).isEqualByComparingTo("129.90");
    }

    private RiskDecisionCreatedEvent paymentEvent(String userId, String deviceId, String ip) {
        return new RiskDecisionCreatedEvent(
                "REQ1",
                "D1",
                "E1",
                "PAYMENT",
                userId,
                deviceId,
                ip,
                new BigDecimal("129.90"),
                "BIZ1",
                "APP",
                LocalDateTime.of(2026, 5, 1, 10, 30),
                "REVIEW",
                70,
                "HIGH",
                1L,
                1,
                "C1",
                LocalDateTime.of(2026, 5, 1, 10, 30)
        );
    }
}
