package com.ruankao.domain.enums;

/**
 * 错题本条目状态（{@code user_wrong_question.status}）。
 *
 * <p>收录与移出规则见 04 §4.3、06 §6.8：答错即收录（{@code wrong_count + 1}）；
 * 答对令 {@code consecutive_correct + 1}，<b>连续答对 2 次</b>置为 {@code MASTERED}；
 * 用户也可手动移出置 {@code REMOVED}。两条规则的区别决定列表口径：
 * {@code GET /wrong-questions} 默认只返回 {@code ACTIVE}，{@code MASTERED}/{@code REMOVED}
 * 需显式传参才可查（05 §5.7）。
 *
 * <p>"连续答对 2 次"的阈值是可配置项（{@code sys_config} 的
 * {@code wrongbook.master.threshold}，默认 2，06 §6.8），故不要在代码里写死 2。
 */
public enum WrongQuestionStatus {

    /** 生效中，出现在错题本可用列表里。 */
    ACTIVE,

    /** 已掌握：连续答对达阈值后移出可用列表，但保留数据。 */
    MASTERED,

    /** 用户手动移除。 */
    REMOVED;

    /** 与库中字符串比较，避免各处散落 {@code "ACTIVE".equals(...)} 字面量。 */
    public boolean matches(String raw) {
        return name().equals(raw);
    }
}