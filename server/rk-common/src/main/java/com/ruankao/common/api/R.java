package com.ruankao.common.api;

import org.slf4j.MDC;

/**
 * 统一响应体（05 §5.1.1）。
 *
 * <p>HTTP 状态码始终为 200，只有鉴权失败（401）与无权限（403）例外，便于前端拦截器统一处理。
 *
 * <p><b>不要给这个 record 加 {@code isXxx()} 形式的方法</b>：Jackson 会把它们当成布尔属性
 * 一起序列化，响应体就不止 {@code code/message/data/traceId} 四个字段了。
 * 需要判断成功与否请比较 {@code code() == 0}。
 *
 * @param code    0 表示成功，非 0 为 {@link ErrorCode} 的码值
 * @param message 面向用户的可读文案
 * @param data    业务数据，可为 null
 * @param traceId 链路标识，供用户反馈问题时定位日志
 */
public record R<T>(int code, String message, T data, String traceId) {

    /** {@code TraceIdFilter} 写入 MDC 时使用的键名，也是日志模板里的占位符名。 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, currentTraceId());
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode, null);
    }

    /** 用带数据的失败响应（如 1001 携带字段级错误列表）。 */
    public static <T> R<T> fail(ErrorCode errorCode, T data) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), data, currentTraceId());
    }

    /**
     * 覆盖错误码自带的文案。仅用于需要拼接具体信息的场景，
     * 例如「该科目尚未配置试卷模板」补充为「科目【软件设计师】尚未配置试卷模板」。
     */
    public static <T> R<T> fail(ErrorCode errorCode, String message, T data) {
        return new R<>(errorCode.getCode(), message, data, currentTraceId());
    }

    /**
     * 读取当前请求的 traceId。MDC 未初始化时（如定时任务、单元测试、CLI 运行）返回空串——
     * 这些场景没有请求上下文，返回 null 会让前端多一层判空。
     */
    private static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        return traceId == null ? "" : traceId;
    }
}
