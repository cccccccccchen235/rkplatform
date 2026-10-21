package com.ruankao.common.exception;

import com.ruankao.common.api.ErrorCode;

/**
 * 业务异常（06 §6.2）。
 *
 * <p>Service 层表达"业务规则不满足"的唯一方式：抛本异常，由 {@code GlobalExceptionHandler}
 * 转成 {@link com.ruankao.common.api.R}。<b>Controller 与 Service 都不写 try-catch</b>。
 *
 * <p>不填充堆栈：业务异常是预期内的控制流（如"该科目尚未配置试卷模板"），
 * 每次都在此构造 20 帧堆栈是纯粹的浪费。真正的缺陷走兜底 {@code Exception} 分支，
 * 那条路径有完整堆栈。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage(), null, false, false);
        this.errorCode = errorCode;
    }

    /**
     * 覆盖错误码自带文案，用于补充具体上下文。
     * 注意：覆盖后的文案是**面向用户**的，不得包含内部标识（堆栈、SQL、字段名以外的东西）。
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
