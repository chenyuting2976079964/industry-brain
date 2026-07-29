package com.industrybrain.interceptor;

import com.industrybrain.common.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 区域数据隔离拦截器
 * 在 SQL/ES/Neo4j 查询中自动追加 region_code 过滤，防止越权访问
 */
@Component
public class RegionDataIsolationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RegionDataIsolationInterceptor.class);

    /**
     * 区域编码请求头名称
     */
    public static final String REGION_CODE_HEADER = "X-Region-Code";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO: 从当前用户上下文获取 regionCode
        // 1. 从 SecurityContext 获取用户区域权限
        // 2. 将 regionCode 存入 ThreadLocal
        // 3. MyBatis/ES/Neo4j 查询时自动附加 region_code 条件
        String regionCode = request.getHeader(REGION_CODE_HEADER);
        if (regionCode != null) {
            log.debug("区域数据隔离: regionCode={}", regionCode);
            // TODO: 设置到 ThreadLocal 供 Mapper 层使用
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        // TODO: 清理 ThreadLocal
    }
}
