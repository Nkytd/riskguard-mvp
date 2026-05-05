package com.riskguard.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.riskguard.auth.entity.SysUser;
import com.riskguard.auth.mapper.SysUserMapper;
import com.riskguard.common.security.RiskUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SysUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public SysUserDetailsService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (sysUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return RiskUserPrincipal.from(sysUser);
    }
}
