package com.shop.common.result;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应包装")
public record Result<T>(
        @Schema(description = "状态码", example = "200")
        int code,

        @Schema(description = "提示消息", example = "操作成功")
        String message,

        @Schema(description = "数据负载")
        T data,

        @Schema(description = "响应时间戳", example = "1730625600000")
        long timestamp
) {
    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, System.currentTimeMillis());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.BIZ_ERROR.getCode(), message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, System.currentTimeMillis());
    }
}
