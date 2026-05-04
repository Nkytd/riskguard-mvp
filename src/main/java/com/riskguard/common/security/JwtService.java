package com.riskguard.common.security;

import com.riskguard.common.utils.ClockUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expireMinutes;

    public JwtService(@Value("${riskguard.security.jwt-secret}") String jwtSecret,
                      @Value("${riskguard.security.jwt-expire-minutes}") long expireMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    public JwtToken generateToken(RiskUserPrincipal principal) {
        LocalDateTime issuedAt = ClockUtils.now();
        LocalDateTime expiresAt = issuedAt.plusMinutes(expireMinutes);
        String token = Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.id())
                .claim("realName", principal.realName())
                .claim("roleCode", principal.roleCode())
                .issuedAt(toDate(issuedAt))
                .expiration(toDate(expiresAt))
                .signWith(secretKey)
                .compact();
        return new JwtToken(token, expiresAt);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    public record JwtToken(
            String accessToken,
            LocalDateTime expiresAt
    ) {
    }
}
