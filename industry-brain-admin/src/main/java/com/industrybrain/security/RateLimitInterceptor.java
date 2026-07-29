package com.industrybrain.security;

import com.industrybrain.common.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器
 * 基于IP+用户的令牌桶限流，防止并发压力和暴力破解
 * Redis 不可用时降级为直接放行
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** 默认每秒令牌数 */
    private static final int DEFAULT_RATE = 100;

    /** 令牌桶容量 */
    private static final int BURST_SIZE = 200;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String clientIp = ServletUtils.getClientIp(request);
        String key = "rate_limit:" + clientIp;

        // TODO: 基于 Redis 滑动窗口/令牌桶实现限流（需 Redis 可用）
        // 当前降级：不启用限流
        return true;
    }

    /**
     * 判断 Redis 是否可用（用于内部逻辑检查）
     */
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }
}
