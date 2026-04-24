package com.zust.qyf.careeragent.domain.dto;

import lombok.Data;

/**
 * 统一 API 响应封装类
 */
@Data
public class Result<T> {
    // 状态码：200 成功，500 失败，400 参数错误
    private final Integer code;
    private final String message;
    private final T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 业务执行成功，状态码: 200
     * @param data 业务数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 业务执行失败
     * @param code 失败状态码 (参数错误: 400, 没有权限: 401, 选择不下来就写500)
     * @param message 失败详细信息, 主动捕获就正常输出, 被全局抓到就 e.message
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
