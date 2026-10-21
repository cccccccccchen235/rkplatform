package com.ruankao.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 卷面模板整体覆盖式提交（05 §5.10 第 52 条）。
 *
 * <p>结构与 {@link com.ruankao.domain.vo.PaperTemplateDetailVO} 逐字对应——
 * {@code GET} 的响应可以直接当作 {@code PUT} 的请求体回填（多出来的只读字段被 Jackson 忽略）。
 * 不设计成"模板 + 独立的场次接口 + 独立的分节接口"：模板是一个整体，
 * 分开写会出现"场次已删、分节还指着它"的中间态，而整体覆盖式提交天然是原子的。
 *
 * <p>模板级<b>没有</b> {@code totalDuration}：它随 {@code exam_paper_template.total_duration}
 * 从 DDL 删除，总时长下沉到各场次（对应评审 A5）。
 *
 * <p>这里只判"非空、范围、长度"，与业务规则（序号连续、时长自洽、满分口径一致）相关的校验
 * 一律在服务层做：那些规则要读 02 §2.4.2 的口径表，放在 DTO 里会让 DTO 变成一个
 * 半懂业务的东西，而错误消息也需要逐条说清"哪个场次、期望多少、实际多少"。
 */
public record PaperTemplateSaveRequest(

        @NotBlank(message = "模板名不能为空")
        @Size(max = 64, message = "模板名过长")
        String name,

        @NotBlank(message = "卷面类型不能为空")
        String paperType,

        @NotNull(message = "卷面满分不能为空")
        @Min(value = 0, message = "卷面满分不能为负")
        Integer totalMaxScore,

        @NotNull(message = "可判分满分不能为空")
        @Min(value = 0, message = "可判分满分不能为负")
        Integer autoScoredMaxScore,

        @NotEmpty(message = "至少需要一个场次")
        @Size(max = 2, message = "最多两个场次")
        @Valid
        List<Sitting> sittings) {

    /**
     * 场次（05 §5.10 第 52 条）。{@code sittingNo} 由调用方给，服务端强制"从 1 连续递增"。
     *
     * <p>{@code totalMaxScore} / {@code autoScoredMaxScore} 由服务端按分节重算并比对，
     * 对不上即 1001——这正是 10 §10.7 第一条验收项「不符时保存失败」。
     */
    public record Sitting(

            @NotNull(message = "场次序号不能为空")
            @Min(value = 1, message = "场次序号从 1 开始")
            Integer sittingNo,

            @NotBlank(message = "场次名不能为空")
            @Size(max = 64, message = "场次名过长")
            String sittingName,

            @NotNull(message = "场次总时长不能为空")
            @Min(value = 1, message = "场次总时长至少 1 分钟")
            Integer totalDuration,

            @NotNull(message = "场次满分不能为空")
            @Min(value = 0, message = "场次满分不能为负")
            Integer totalMaxScore,

            @NotNull(message = "场次可判分满分不能为空")
            @Min(value = 0, message = "场次可判分满分不能为负")
            Integer autoScoredMaxScore,

            @NotNull(message = "场次排序号不能为空")
            @Min(value = 0, message = "场次排序号不能为负")
            Integer sortNo,

            @NotEmpty(message = "每个场次至少需要一个分节")
            @Size(max = 3, message = "每个场次最多三个分节")
            @Valid
            List<Section> sections) {
    }

    /**
     * 分节（05 §5.10 第 52 条）。
     *
     * <p>不带 {@code sittingNo}：它由"嵌套在哪个场次里"表达，多一个输入就多一种自相矛盾
     * （数组里写着场次 1、{@code sittingNo} 写着 2）。
     *
     * <p>{@code durationMin}/{@code durationMax} <b>均必填</b>（对应评审 A6）：分节不存在
     * "不需要时长约束"的场景，DDL 也是 {@code NOT NULL}。
     */
    public record Section(

            @NotNull(message = "分节序号不能为空")
            @Min(value = 1, message = "分节序号从 1 开始")
            Integer sectionNo,

            @NotBlank(message = "分节名不能为空")
            @Size(max = 64, message = "分节名过长")
            String sectionName,

            @NotBlank(message = "分节类型不能为空")
            String sectionType,

            @NotBlank(message = "题型不能为空")
            String questionType,

            @NotNull(message = "题目数不能为空")
            @Min(value = 0, message = "题目数不能为负")
            Integer questionCount,

            @NotNull(message = "每题分值不能为空")
            @DecimalMin(value = "0", message = "每题分值不能为负")
            BigDecimal perQuestionScore,

            @NotNull(message = "最早可离开时间不能为空")
            @Min(value = 0, message = "最早可离开时间不能为负")
            Integer durationMin,

            @NotNull(message = "最晚可离开时间不能为空")
            @Min(value = 0, message = "最晚可离开时间不能为负")
            Integer durationMax,

            @NotNull(message = "是否允许提前交卷不能为空")
            Boolean allowEarlySubmit,

            @NotBlank(message = "支持状态不能为空")
            String supportStatus,

            @NotNull(message = "分节排序号不能为空")
            @Min(value = 0, message = "分节排序号不能为负")
            Integer sortNo) {
    }
}
