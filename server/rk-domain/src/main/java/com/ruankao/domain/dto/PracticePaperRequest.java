package com.ruankao.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 练习取卷请求（05 §5.5 第 12 条 {@code POST /practice/papers}）。
 *
 * <p>用可变 Lombok 类而非 record：六个模式里只有 {@code CHAPTER} 需要 {@code refId}
 * 传章节、{@code KP} 需要 {@code refId} 传知识点、{@code SEQUENCE} 需要 {@code fromSequence}
 * 续做，其余字段缺省即由 Service 兜底；"全字段构造"会让人误以为每个模式都要把字段填满。
 *
 * <p>{@code subjectId} 恒为必填：05 §5.5 规定取卷必须落在某个科目内，不存在"跨科目混合卷"。
 *
 * <p><b>{@code count} 只在这里拦 1~50，{@code SAMPLE} 的"至多 5 题"不在此处用注解限制</b>：
 * 游客样例超量时接口要返回业务码 {@code 1001}，且 {@code reason} 与"一次最多 50 题"不同，
 * 由 Service 在识别出 {@code mode == SAMPLE} 后单独判定并给出对应理由；注解只负责所有模式
 * 共有的硬边界。缺省题量 10（05 §5.5）。
 *
 * @param mode {@code CHAPTER/KP/SEQUENCE/FAVORITE/WRONG_REDO/SAMPLE}，见
 *             {@link com.ruankao.domain.enums.PracticeMode}。
 *             <b>刻意不加 {@code @Pattern}</b>：若在这里把未知 mode 拦成 1001，
 *             "未登录 + mode=FULL" 就永远拿不到 2005（05 §5.13 的测试要求
 *             「未登录 + mode=FULL → 2005」），而那条要求正是为了证明
 *             安全判断发生在方法体内、而不是被白名单或注解提前短路。
 *             所以 mode 的合法性判断放在 Service 里，排在"游客资格"判断之后。
 * @param refId 取题范围引用：{@code CHAPTER} 传章节 ID、{@code KP} 传知识点 ID，
 *              其余模式为 {@code null}。
 * @param difficulty 期望难度 1~5（1 最易），缺省不限。
 * @param onlyUnmastered 仅取未掌握题（错题重做等的"只做未掌握"开关）；{@code null} 视为 {@code false}。
 * @param fromSequence {@code SEQUENCE} 续做起点（题序，从 0 起）；缺省从头开始。
 */
@Getter
@Setter
@NoArgsConstructor
public class PracticePaperRequest {

    @NotNull(message = "科目 ID 不能为空")
    private String subjectId;

    @NotBlank(message = "练习模式不能为空")
    private String mode;

    /** 取题范围引用；{@code CHAPTER}/{@code KP} 必填，其余模式忽略。 */
    private String refId;

    /**
     * 题量；缺省 10，上限 50（05 §5.5）。
     *
     * <p><b>{@code SAMPLE} 的 count ≤ 5 不在这里用注解拦</b>——违规时要返回 {@code 1001}
     * 且理由与"超过 50"不同，由 Service 判定。
     */
    @Min(value = 1, message = "题量至少 1")
    @Max(value = 50, message = "题量最多 50")
    private Integer count;

    /** 期望难度；{@code null} 表示不限。 */
    @Min(value = 1, message = "难度最小为 1")
    @Max(value = 5, message = "难度最大为 5")
    private Integer difficulty;

    /** 仅取未掌握题；{@code null} 视为否。 */
    private Boolean onlyUnmastered;

    /** {@code SEQUENCE} 续做起点（题序）；{@code null} 或 0 表示从头。 */
    @Min(value = 0, message = "续做起点不能为负")
    private Integer fromSequence;
}