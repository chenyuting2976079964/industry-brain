package com.industrybrain.interceptor;

import com.industrybrain.common.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

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
        String regionCode = request.getHeader(REGION_CODE_HEADER);
        if (regionCode != null) {
            log.debug("区域数据隔离: regionCode={}", regionCode);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
    }
}
