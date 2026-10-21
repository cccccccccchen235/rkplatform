package com.ruankao.domain.enums;

/**
 * 题目反馈类型（{@code rk_question_feedback.type}）。
 *
 * <p>取值与含义见 02 §2.6.3、04 §4.4。其中 {@code COPYRIGHT}（版权投诉）在管理端标记为
 * <b>最高优先级</b>，处理动作是下架题目（{@code PUBLISHED → OFFLINE}）（02 §2.6.3）；
 * 提交入口与字段见 05 §5.9。
 */
public enum FeedbackType {

    /** 错别字。 */
    TYPO,

    /** 答案存疑。 */
    WRONG_ANSWER,

    /** 解析不清。 */
    UNCLEAR_ANALYSIS,

    /** 版权投诉：最高优先级，处理动作= 下架。 */
    COPYRIGHT,

    /** 其他。 */
    OTHER;

    /** 与库中字符串比较，避免各处散落 {@code "COPYRIGHT".equals(...)} 字面量。 */
    public boolean matches(String raw) {
        return name().equals(raw);
    }
}