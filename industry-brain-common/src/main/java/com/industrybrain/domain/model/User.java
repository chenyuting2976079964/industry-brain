package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户实体 (user表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码 (BCrypt) */
    @TableField("password")
    private String passwordHash;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色编码: ADMIN/OPERATOR/VIEWER */
    @TableField("role_code")
    private String role;

    /** 所属区域ID */
    private Long regionId;

    /** 状态: 1正常/0禁用 */
    @TableField("status")
    private Integer isActive;

    /** 登录失败次数 */
    private Integer loginFailCount;

    /** 锁定到期时间 */
    @TableField("lock_until")
    private LocalDateTime lockedUntil;

    /** 密码最后修改时间 */
    private LocalDateTime passwordUpdatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
