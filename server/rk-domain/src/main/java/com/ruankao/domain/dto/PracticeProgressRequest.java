package com.ruankao.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 练习进度上报请求（05 §5.5 第 15 条 {@code PUT /practice/progress}）。
 *
 * <p>用可变类而非 record：进度是"整条覆盖"的当前游标，Service 按
 * {@code (user_id, subject_id, practice_type, ref_id)} 做 UPSERT，缺省字段由 Service 补齐。
 *
 * <p><b>{@code mode} 白名单不含 {@code SAMPLE}</b>：游客没有持久化进度，样例练习
 * 既不登录也无 {@code user_id} 可挂载。把 {@code SAMPLE} 从取值里剔除，比在 Service 里
 * 再判一次"是游客就跳过"更能在入口处拦住脏数据。
 *
 * @param refId 进度所属范围：{@code CHAPTER} 为章节 ID、{@code KP} 为知识点 ID，
 *              其余模式按各自范围取值，可为 {@code null}。
 * @param lastSequence 已推进到的题序（从 0 起），下次续做从这里继续。
 * @param lastQuestionId 最后作答的题目 ID，用于"继续上次"精确定位；可为 {@code null}。
 */
@Getter
@Setter
@NoArgsConstructor
public class PracticeProgressRequest {

    @NotNull(message = "科目 ID 不能为空")
    private String subjectId;

    @NotBlank(message = "练习模式不能为空")
    @Pattern(regexp = "CHAPTER|KP|SEQUENCE|FAVORITE|WRONG_REDO", message = "练习模式非法")
    private String mode;

    /** 进度范围引用；按模式语义取值，可为 {@code null}。 */
    private String refId;

    @NotNull(message = "题序不能为空")
    @Min(value = 0, message = "题序不能为负")
    private Integer lastSequence;

    /** 最后作答的题目 ID；可为 {@code null}。 */
    private String lastQuestionId;
}