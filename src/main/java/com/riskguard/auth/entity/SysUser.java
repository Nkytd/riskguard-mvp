package com.riskguard.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_user")
public class SysUser {

    @TableId
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private String roleCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
