package com.industrybrain.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 响应结果封装
 * 若依风格：统一 AjaxResult 返回格式
 */
@Data
public class AjaxResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 返回信息 */
    private String msg;

    /** 返回数据 */
    private Object data;

    public AjaxResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功", null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    public static AjaxResult error() {
        return new AjaxResult(500, "操作失败", null);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(500, msg, null);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }
}
