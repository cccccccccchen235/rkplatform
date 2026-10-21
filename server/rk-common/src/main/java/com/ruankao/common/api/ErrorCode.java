package com.ruankao.common.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局错误码。
 *
 * <p>取值与 {@code docs/05-api.md §5.1.2} 的错误码表一一对应。新增错误码必须两处同步，
 * 否则前端拿到一个文档里查不到的数字。分段：
 *
 * <pre>
 *   0    成功
 *   1xxx 通用
 *   2xxx 认证与用户
 *   3xxx 题库与内容
 *   4xxx 考试与判分
 *   5xxx 统计
 * </pre>
 *
 * <p>{@code httpStatus} 只对 401/403 有意义——其余业务错误一律 HTTP 200，
 * 由前端拦截器按 {@code code} 分流（05 §5.1.1）。
 */
public enum ErrorCode {

    SUCCESS(0, "ok", 200),

    // ---------- 1xxx 通用 ----------
    SYSTEM_ERROR(1000, "系统内部错误", 200),
    PARAM_INVALID(1001, "参数校验失败", 200),
    TOO_MANY_REQUESTS(1002, "请求过于频繁，请稍后再试", 200),
    RESOURCE_NOT_FOUND(1003, "资源不存在", 200),
    FORBIDDEN(1004, "无权限访问该资源", 403),
    STATUS_NOT_ALLOWED(1005, "当前状态不允许该操作", 200),

    // ---------- 2xxx 认证与用户 ----------
    LOGIN_FAILED(2001, "用户名或密码错误", 200),
    ACCOUNT_DISABLED(2002, "账号已被禁用", 200),
    USERNAME_EXISTS(2003, "用户名已存在", 200),
    LOGIN_LOCKED(2004, "登录失败次数过多，请 15 分钟后再试", 200),
    NOT_LOGGED_IN(2005, "未登录或登录已过期", 401),
    OLD_PASSWORD_WRONG(2006, "原密码错误", 200),

    // ---------- 3xxx 题库与内容 ----------
    SUBJECT_UNAVAILABLE(3001, "科目不存在或已停用", 200),
    QUESTION_NOT_FOUND(3002, "题目不存在", 200),
    QUESTION_STATUS_NOT_ALLOWED(3003, "题目状态不允许此操作", 200),
    QUESTION_INCOMPLETE(3004, "题目内容不完整（如客观题缺少正确答案或选项）", 200),
    QUESTION_DUPLICATE(3005, "检测到高度相似的重复题目", 200),

    // ---------- 4xxx 考试与判分 ----------
    PAPER_TEMPLATE_MISSING(4001, "该科目尚未配置试卷模板", 200),
    PAPER_NO_ANSWERABLE_SECTION(4002, "该科目卷面没有可作答的分节", 200),
    SESSION_UNFINISHED_EXISTS(4003, "存在未完成的模拟考，请先交卷或放弃", 200),
    SESSION_NOT_FOUND(4004, "模拟考会话不存在", 200),
    SESSION_ALREADY_ENDED(4005, "会话已结束，不可再作答", 200),
    QUESTION_NOT_IN_SESSION(4006, "题目不属于该会话", 200),
    SECTION_NOT_SUPPORTED(4007, "该分节在当前平台不可作答", 200),

    // ---------- 5xxx 统计 ----------
    NO_STATISTICS(5001, "暂无统计数据", 200);

    private static final Map<Integer, ErrorCode> BY_CODE;

    static {
        Map<Integer, ErrorCode> map = new HashMap<>();
        for (ErrorCode ec : values()) {
            ErrorCode prev = map.put(ec.code, ec);
            if (prev != null) {
                // 码值重复会让前端的分支判断出现随机行为，属于必须在启动前暴露的错误。
                throw new IllegalStateException("ErrorCode 码值重复: " + ec.code);
            }
        }
        BY_CODE = Collections.unmodifiableMap(map);
    }

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** 按码值反查。未登记的码值返回 {@link #SYSTEM_ERROR}，避免把未知码继续往外传。 */
    public static ErrorCode fromCode(int code) {
        return BY_CODE.getOrDefault(code, SYSTEM_ERROR);
    }
}
