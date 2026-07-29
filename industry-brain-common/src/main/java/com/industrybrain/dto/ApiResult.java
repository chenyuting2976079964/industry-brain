package com.industrybrain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private Integer code;    // 200=success
    private String msg;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder().code(200).msg("success").data(data).build();
    }

    public static <T> ApiResult<T> success() {
        return ApiResult.<T>builder().code(200).msg("success").build();
    }

    public static <T> ApiResult<T> error(String msg) {
        return ApiResult.<T>builder().code(500).msg(msg).build();
    }

    public static <T> ApiResult<T> error(Integer code, String msg) {
        return ApiResult.<T>builder().code(code).msg(msg).build();
    }
}
