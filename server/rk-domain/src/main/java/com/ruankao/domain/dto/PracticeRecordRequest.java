package com.ruankao.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 练习作答上报请求（05 §5.5 第 13 条 {@code POST /practice/records}）。
 *
 * <p>用可变类而非 record：上报是"批量追加"语义，必须支持空集合校验与逐条嵌套校验，
 * 可变类 + {@code @Valid} 是 Spring 校验里最直接的路径。
 *
 * <p><b>请求体里刻意没有 {@code isCorrect} 字段</b>——练习模式由前端即时判分给用户看，
 * 但服务端在上报时必须用 {@code JudgeService} 重新判一遍（AC-3：客户端上报的对错不可信）。
 * 把这个字段从契约上删掉，比"记得忽略它"可靠：届时没有这个字段可填，也就不可能有人误用。
 *
 * <p>{@code items} 单次上限 50，与 {@link PracticePaperRequest} 的题量上限一致；
 * 客户端应分批上报，而不是把一整套模拟考的作答塞进一次请求。
 *
 * <p>{@code source} 是来源白名单，见 {@link com.ruankao.domain.enums.PracticeSource}：
 * 注意 {@code WRONG_REDO} 不带 {@code PRACTICE_} 前缀（既有约定，勿"顺手对齐"，
 * 否则历史数据与统计口径全部失配）。
 */
@Getter
@Setter
@NoArgsConstructor
public class PracticeRecordRequest {

    @NotNull(message = "科目 ID 不能为空")
    private String subjectId;

    @NotBlank(message = "答题来源不能为空")
    @Pattern(regexp = "PRACTICE_CHAPTER|PRACTICE_KP|PRACTICE_SEQUENCE|PRACTICE_FAVORITE|WRONG_REDO",
             message = "答题来源非法")
    private String source;

    @NotEmpty(message = "上报内容不能为空")
    @Size(max = 50, message = "单次最多上报 50 条")
    @Valid
    private List<Item> items;

    /**
     * 单条作答。
     *
     * <p>{@code userAnswer} 长度上限 512 对齐 DDL {@code user_answer VARCHAR(512)}（04 §4.3）：
     * 在这里拦住超长输入，比让它在写库时被截断/报错更早、更明确。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {

        @NotBlank(message = "题目 ID 不能为空")
        private String questionId;

        /** 用户作答；客观题为选项 key（如 {@code ABD}），主观题为文本。 */
        @Size(max = 512, message = "作答内容过长")
        private String userAnswer;

        /** 作答耗时（毫秒），用于学习行为统计。 */
        @Min(value = 0, message = "作答耗时不能为负")
        private Integer durationMs;
    }
}