package com.riskguard.common.security;

import com.riskguard.auth.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record RiskUserPrincipal(
        Long id,
        String username,
        String password,
        String realName,
        String roleCode,
        String status
) implements UserDetails {

    public static RiskUserPrincipal from(SysUser sysUser) {
        return new RiskUserPrincipal(
                sysUser.getId(),
                sysUser.getUsername(),
                sysUser.getPasswordHash(),
                sysUser.getRealName(),
                sysUser.getRoleCode(),
                sysUser.getStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
