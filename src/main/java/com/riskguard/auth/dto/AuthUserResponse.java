package com.riskguard.auth.dto;

import com.riskguard.common.security.RiskUserPrincipal;

public record AuthUserResponse(
        Long id,
        String username,
        String realName,
        String roleCode
) {

    public static AuthUserResponse from(RiskUserPrincipal principal) {
        return new AuthUserResponse(
                principal.id(),
                principal.getUsername(),
                principal.realName(),
                principal.roleCode()
        );
    }
}
