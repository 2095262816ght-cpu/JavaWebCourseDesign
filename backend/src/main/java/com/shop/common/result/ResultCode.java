package com.shop.common.result;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "尚未登录或登录凭证已失效"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求资源未找到"),
    INTERNAL_ERROR(500, "系统内部繁忙，请稍后再试"),
    BIZ_ERROR(1000, "业务处理失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
