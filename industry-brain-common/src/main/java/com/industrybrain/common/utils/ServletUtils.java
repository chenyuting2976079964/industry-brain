package com.industrybrain.common.utils;

import javax.servlet.http.HttpServletRequest;

/**
 * Servlet 工具类
 * @author 何锦鹏
 */
public final class ServletUtils {
    private ServletUtils() {}

    private static final String[] IP_HEADERS = {
        "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_CLIENT_IP",
        "REMOTE_ADDR"
    };

    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
