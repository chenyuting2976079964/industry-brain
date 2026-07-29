package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户Token实体 (user_token表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_token")
public class UserToken {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** Token值 */
    private String token;

    /** Token类型: ACCESS / REFRESH */
    private String tokenType;

    /** 过期时间 */
    @TableField("expires_at")
    private LocalDateTime expireAt;

    /** 是否已撤销: 0未撤销/1已撤销 */
    private Integer isRevoked;

    private LocalDateTime createdAt;
}
