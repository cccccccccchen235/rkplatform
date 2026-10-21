package com.ruankao.domain.enums;

/**
 * 题型（{@code rk_question.question_type}）。
 *
 * <p>五种取值的判分口径见 02 §2.2.1、06 §6.7：{@code SINGLE_CHOICE} 严格相等判分；
 * {@code MULTI_CHOICE} 集合完全相等才得分、不给部分分；{@code TRUE_FALSE} 自动判分。
 * {@code SUBJECTIVE}（主观题）与 {@code ESSAY}（论文）<b>不判分、也不自评打分</b>
 * （02 §2.4.4）：对应的答题记录 {@code user_answer_record.is_correct} 落库为 {@code NULL}
 * （04 §4.3）。把它们放进同一枚举，是因为判分引擎对客观三型走同一段代码路径
 * （仅比较逻辑不同，06 §6.7），拆开会让将来的比较逻辑重新分支。
 *
 * <p>{@code MULTI_CHOICE}/{@code TRUE_FALSE} 是模型层预留而非产品需求：官方客观题全部是
 * 单选题，生成流水线默认只产出 {@code SINGLE_CHOICE}（02 §2.2.1）。
 */
public enum QuestionType {

    /** 单选题：主力，唯一默认生成题型。 */
    SINGLE_CHOICE,

    /** 多选题：预留，集合完全相等才得分。 */
    MULTI_CHOICE,

    /** 判断题：预留。 */
    TRUE_FALSE,

    /** 主观题：不判分，展示参考答案与评分要点。 */
    SUBJECTIVE,

    /** 论文：不判分，提供计时写作框与实时字数统计。 */
    ESSAY;

    /** 与库中字符串比较，避免各处散落 {@code "SINGLE_CHOICE".equals(...)} 字面量。 */
    public boolean matches(String raw) {
        return name().equals(raw);
    }

    /**
     * 是否客观题（需自动判分）。前三种题型恒为 {@code true}；
     * {@code SUBJECTIVE}/{@code ESSAY} 为 {@code false}，其 {@code is_correct} 固定落 {@code NULL}。
     */
    public boolean isObjective() {
        return this == SINGLE_CHOICE || this == MULTI_CHOICE || this == TRUE_FALSE;
    }
}