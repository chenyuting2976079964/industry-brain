package com.industrybrain.framework.exception;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 若依风格：统一捕获异常并返回 AjaxResult
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public AjaxResult handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public AjaxResult handleConstraintViolation(javax.validation.ConstraintViolationException e) {
        log.error("参数校验异常: {}", e.getMessage());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 权限异常（Spring Security）
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.error("权限异常: {}", e.getMessage());
        return AjaxResult.error(403, "权限不足");
    }

    /**
     * 认证异常（Spring Security）
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public AjaxResult handleBadCredentials(org.springframework.security.authentication.BadCredentialsException e) {
        log.error("认证异常: {}", e.getMessage());
        return AjaxResult.error(401, "账号或密码错误");
    }

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        log.error("未知异常", e);
        return AjaxResult.error("系统繁忙，请稍后再试");
    }
}
