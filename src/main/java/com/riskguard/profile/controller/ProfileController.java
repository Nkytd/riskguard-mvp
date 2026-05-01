package com.riskguard.profile.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.profile.dto.DeviceProfileResponse;
import com.riskguard.profile.dto.IpProfileResponse;
import com.riskguard.profile.dto.UserProfileResponse;
import com.riskguard.profile.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        return ApiResponse.success(profileService.getUserProfile(userId));
    }

    @GetMapping("/devices/{deviceId}")
    public ApiResponse<DeviceProfileResponse> getDeviceProfile(@PathVariable String deviceId) {
        return ApiResponse.success(profileService.getDeviceProfile(deviceId));
    }

    @GetMapping("/ips/{ip}")
    public ApiResponse<IpProfileResponse> getIpProfile(@PathVariable String ip) {
        return ApiResponse.success(profileService.getIpProfile(ip));
    }
}
