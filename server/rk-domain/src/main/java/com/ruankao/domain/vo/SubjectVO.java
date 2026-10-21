package com.ruankao.domain.vo;

/**
 * 科目（05 §5.2）。字段即契约，改动需同步 05 与本类。
 *
 * <p>{@code id} 是 {@code String} 而非 {@code Long}：主键为 {@code BIGINT UNSIGNED}，
 * 一旦超过 2^53 前端 JS 会静默丢精度。转换器负责 {@code String.valueOf}，
 * 不在 VO 上挂 Jackson 序列化器——那会让"哪些字段是 ID"这件事从类型上消失。
 *
 * @param paperTotalScore 卷面满分（如 225 / 150 / 75）。**列表接口就返回**，因为它是一个整数、
 *                        不是一棵树；列表页需要它来决定卷面规模的呈现（分节数、能否发起模拟考）。
 *                        为 {@code null} 表示没有启用中的卷面模板——前端据此判断"能不能考"，
 *                        不必再为它单独开一个布尔字段。
 *                        <p><b>不由 {@code paperType} 换算得来</b>：{@code paperType} 只有
 *                        {@code JUNIOR_MID}/{@code SENIOR} 两值，而 IPT 的满分是 75 而非 150
 *                        （它的「应用技术」节不予机考，NOT_SUPPORTED 分节不计入满分）。
 *                        这个例外是数据事实，让前端去写死 {@code code = 'IPT'} 是错的。
 * @param paperSummary 卷面结构摘要。<b>列表接口返回 null</b>，仅 {@code GET /subjects/{id}}
 *                     填充（05 §5.4 第 9 条把"含 paperSummary"列为详情接口的特性）；
 *                     列表页只需要科目标题，没必要为 20 个科目各带一棵场次树。
 */
public record SubjectVO(String id,
                        String level,
                        String code,
                        String name,
                        String paperType,
                        String examPeriod,
                        String outlineStatus,
                        String contentStatus,
                        Integer questionCount,
                        Integer paperTotalScore,
                        String intro,
                        PaperSummaryVO paperSummary) {
}
