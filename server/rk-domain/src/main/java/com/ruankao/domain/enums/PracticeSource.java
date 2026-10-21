package com.ruankao.domain.enums;

/**
 * 答题记录来源（{@code user_answer_record.source}）。
 *
 * <p>取值集合即 DDL 注释里列出的六个，是本列<b>唯一允许的取值</b>（04 §4.3）。
 * 前端练习模式到本枚举的映射见 07 前端上报映射：
 * {@code CHAPTER → PRACTICE_CHAPTER}、{@code KP → PRACTICE_KP}、
 * {@code SEQUENCE → PRACTICE_SEQUENCE}、{@code FAVORITE → PRACTICE_FAVORITE}、
 * {@code WRONG_REDO → WRONG_REDO}；{@code SAMPLE}（游客）<b>不上报</b>，故本枚举无对应值。
 *
 * <p>注意这些取值都带 {@code PRACTICE_} 前缀，唯独 {@code WRONG_REDO} 不带——
 * 这是既有约定，不要"顺手对齐"成 {@code PRACTICE_WRONG_REDO}，否则历史数据与统计口径全部失配。
 */
public enum PracticeSource {

    PRACTICE_CHAPTER,

    PRACTICE_KP,

    PRACTICE_SEQUENCE,

    PRACTICE_FAVORITE,

    WRONG_REDO,

    /** 模拟考（只有这一来源的 {@code session_id} 有值，04 §4.3）。 */
    EXAM;

    /** 与库中字符串比较，避免各处散落 {@code "EXAM".equals(...)} 字面量。 */
    public boolean matches(String raw) {
        return name().equals(raw);
    }

    /** 是否为合法取值；用于写库前的兜底校验。 */
    public static boolean isValid(String raw) {
        for (PracticeSource v : values()) {
            if (v.matches(raw)) {
                return true;
            }
        }
        return false;
    }
}