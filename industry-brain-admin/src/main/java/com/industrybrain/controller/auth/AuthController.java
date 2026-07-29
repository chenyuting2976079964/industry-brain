package com.industrybrain.controller.auth;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.security.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录认证控制器
 * 登录、登出、当前用户信息
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> loginBody) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");

        // Simple credential check (admin / 123456)
        // TODO: replace with real user service
        if (!"admin".equals(username) || !"123456".equals(password)) {
            return AjaxResult.error("用户名或密码错误");
        }

        String token = jwtTokenProvider.createToken(1L, username);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", 1);
        user.put("username", "admin");
        user.put("displayName", "管理员");
        user.put("role", "园区运营");
        user.put("avatar", "");
        user.put("phone", "");
        user.put("email", "");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);

        return AjaxResult.success(result);
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public AjaxResult logout() {
        return AjaxResult.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public AjaxResult getUserInfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return AjaxResult.error(401, "未登录");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return AjaxResult.error(401, "Token已过期，请重新登录");
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", 1);
        user.put("username", jwtTokenProvider.getUsernameFromToken(token));
        user.put("displayName", "管理员");
        user.put("role", "园区运营");
        user.put("avatar", "");
        user.put("phone", "");
        user.put("email", "");

        return AjaxResult.success(user);
    }
}
