package com.industrybrain.config;

import com.industrybrain.interceptor.RegionDataIsolationInterceptor;
import com.industrybrain.security.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 注册拦截器、跨域等
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RegionDataIsolationInterceptor regionDataIsolationInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 区域数据隔离拦截器 - 对所有接口生效
        registry.addInterceptor(regionDataIsolationInterceptor)
                .addPathPatterns("/**");

        // 限流拦截器 - 对公开接口生效
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/auth/login", "/industry/**");
    }
}
