package com.industrybrain.common.constant;

/**
 * 通用常量
 */
public class Constants {

    /** 正常状态 */
    public static final String STATUS_NORMAL = "0";

    /** 停用状态 */
    public static final String STATUS_DISABLE = "1";

    /** 超级管理员ID */
    public static final Long SUPER_ADMIN_ID = 1L;

    /** 默认密码 */
    public static final String DEFAULT_PASSWORD = "admin123";

    /** Token 有效期（秒）：7天 */
    public static final int TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /** 管理员角色编码 */
    public static final String ADMIN_ROLE_KEY = "admin";
}
