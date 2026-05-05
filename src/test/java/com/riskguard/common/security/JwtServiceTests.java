package com.riskguard.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTests {

    @Test
    void generateAndParseToken() {
        JwtService jwtService = new JwtService("riskguard-local-development-secret-change-me", 120);
        RiskUserPrincipal principal = new RiskUserPrincipal(
                1L,
                "admin",
                "{noop}RiskGuard@123456",
                "Administrator",
                "ADMIN",
                "ENABLED"
        );

        JwtService.JwtToken token = jwtService.generateToken(principal);
        var claims = jwtService.parseToken(token.accessToken());

        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("realName", String.class)).isEqualTo("Administrator");
        assertThat(claims.get("roleCode", String.class)).isEqualTo("ADMIN");
        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.expiresAt()).isNotNull();
    }
}
