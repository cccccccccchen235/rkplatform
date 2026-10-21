package com.ruankao.repository.cache;

/**
 * 缓存 Key 常量（06 §6.11 的表）。
 *
 * <p>集中在一处而不是散落在各 Service 里拼字符串：缓存的失效只靠"删对同一个键"，
 * 一处拼错就是一条<b>永远不会被清除</b>的缓存——而它的表现是"管理端改了，
 * 考生端一小时后才变"，从现象上看不出是键拼错了。
 *
 * <p>本类登记 {@code rk:cache:*}（P2）与 P4 新增的 {@code rk:repeat:*}、
 * {@code rk:limit:*} 三类键。06 §6.11 表里的其余键在各自功能落地时再补进来，
 * 不在没有写入方的时候先建空常量。
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 科目列表（05 §5.4 第 8 条）。管理端改动科目时 DEL。 */
    public static final String SUBJECT_LIST = "rk:cache:subject:list";

    /** 某科目的大纲树（05 §5.4 第 10 条）。管理端改动该科目章节时 DEL。 */
    public static String outline(Long subjectId) {
        return "rk:cache:outline:" + subjectId;
    }

    /**
     * 组卷防重复窗口的成员集（06 §6.6.4）：该用户在该科目下"最近答过的题"。
     *
     * <p>它<b>不</b>走缓存失效：唯一的清除方式是 TTL 到期，写坏了大不了重新回填
     * （回填口径见 06 §6.6.4 的 SQL）。因此没有对应的 evict 调用点。
     */
    public static String repeatWindow(Long userId, Long subjectId) {
        return "rk:repeat:" + userId + ":" + subjectId;
    }

    /** 题目反馈限流计数器（05 §5.9：同题同用户 24 小时内最多 3 次）。 */
    public static String feedbackLimit(Long userId, Long questionId) {
        return "rk:limit:feedback:" + userId + ":" + questionId;
    }

    /**
     * 统计待重算标记（06 §6.10.2、§6.11）：该用户该科目自上次重算后又答过题。
     *
     * <p>它表达的是一件"不知道也没关系、知道了更准"的事：重算时若标记不存在，
     * 直接跳过；若 Redis 整个不可用（读不到标记），正确方向是<b>当作已脏</b>去重算，
     * 因为重算是幂等的、而漏算会一直错到夜间全量重算为止。
     */
    public static String statStale(Long userId, Long subjectId) {
        return "rk:stat:stale:" + userId + ":" + subjectId;
    }
}
