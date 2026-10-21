package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 题目（{@code rk_question}），题库域核心表。
 *
 * <p>刻意<b>不带任何 MyBatis 注解</b>：06 §6.1 禁止 rk-domain 依赖 MyBatis，
 * 映射写在 rk-repository 的 XML Mapper 里（显式列名 + {@code mapUnderscoreToCamelCase}）。
 *
 * <p>组卷查询的唯一入口条件是 {@code subject_id + status = 'PUBLISHED' + question_type}
 * （04 §4.4）。状态机及各状态的"是否可被组卷"见 02 §2.5.1：<b>仅 {@code PUBLISHED} 可组卷</b>，
 * 其余状态（含 {@code OFFLINE}）都不进新卷，但仍保留历史作答记录。
 *
 * <p>{@code source_type}/{@code source_ref}/{@code source_license}/{@code generation_batch_id}
 * 四列在 DDL 上保持可空，是因为 {@code source_type} 各取值所需字段不同，无法用统一 NOT NULL
 * 表达（对应评审 B7）。非空契约（02 §2.6.1 的 GR-2 全链路来源留痕）改由<b>应用层在
 * {@code PUBLISHED} 前强制</b>，校验点见 05 的发布接口与 08 §8.2.3。
 */
@Getter
@Setter
@NoArgsConstructor
public class Question {

    private Long id;

    private Long subjectId;

    /** 所属叶子章节（{@code rk_outline_node.id}）。 */
    private Long outlineId;

    /** SINGLE_CHOICE / MULTI_CHOICE / TRUE_FALSE / SUBJECTIVE / ESSAY，见 {@link com.ruankao.domain.enums.QuestionType}。 */
    private String questionType;

    /** 题干，Markdown + KaTeX（行内 {@code $...$} 与块级 {@code $$...$$}）。 */
    private String stem;

    /** 目前唯一取值 MARKDOWN，预留 HTML 扩展位（02 §2.2.2）。 */
    private String stemFormat;

    /**
     * 题干插图的 JSON 数组<b>文本</b>，元素形如 {@code {url, alt}}（04 §4.4）。
     *
     * <p>注意：库里是 JSON 列，但本类按 {@code String} 承载，不做结构化映射——
     * 解析内插图直接走题干 Markdown，该数组只用于页眉缩略图等场景，应用层读取时再解析。
     */
    private String images;

    /** 客观题选项 key（多选按字典序拼接，如 {@code ABD}）；主观题为 {@code null}（02 §2.2.2）。 */
    private String correctAnswer;

    /** 主观题/论文参考答案（Markdown）；客观题可为空。 */
    private String referenceAnswer;

    /** 解析（Markdown，支持插图）。 */
    private String analysis;

    /** 主观题评分要点的 JSON 字符串数组文本，如 {@code ["能说明数据流图的四个要素", "..."]}。 */
    private String scorePoints;

    /** 难度 1~5，1 最易、5 最难（以 02 文档为准），用于组卷配比与统计。 */
    private Integer difficulty;

    /** AI_GENERATED / ORIGINAL / LICENSED；必填，不可为空（约束 C1，02 §2.5.1）。 */
    private String sourceType;

    /** 来源说明（批次号或编写人）；AI 生成时记录本次覆盖的考点范围。 */
    private String sourceRef;

    /** 许可标识；AI_GENERATED / ORIGINAL 填 SELF。 */
    private String sourceLicense;

    /** AI 生成时关联的批次（{@code rk_generation_batch.id}）；非 AI 生成为 {@code null}。 */
    private Long generationBatchId;

    /** DRAFT / PENDING_REVIEW / APPROVED / REJECTED / PUBLISHED / OFFLINE，状态机见 02 §2.5.1。 */
    private String status;

    /** 每次内容编辑 +1，供审计。 */
    private Integer version;

    private Long createdBy;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}