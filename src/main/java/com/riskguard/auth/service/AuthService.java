package com.riskguard.auth.service;

import com.riskguard.auth.dto.AuthUserResponse;
import com.riskguard.auth.dto.LoginRequest;
import com.riskguard.auth.dto.LoginResponse;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.common.security.JwtService;
import com.riskguard.common.security.RiskUserPrincipal;
import com.riskguard.common.security.SecurityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            RiskUserPrincipal principal = (RiskUserPrincipal) authentication.getPrincipal();
            JwtService.JwtToken token = jwtService.generateToken(principal);
            return new LoginResponse("Bearer", token.accessToken(), token.expiresAt(), AuthUserResponse.from(principal));
        } catch (BadCredentialsException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Username or password is incorrect");
        } catch (AuthenticationException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, ex.getMessage());
        }
    }

    public AuthUserResponse currentUser() {
        return SecurityUtils.currentPrincipal()
                .map(AuthUserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
