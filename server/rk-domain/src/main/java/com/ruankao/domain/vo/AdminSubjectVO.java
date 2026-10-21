package com.ruankao.domain.vo;

/**
 * 管理端科目列表项（05 §5.2、§5.10 第 37 条）。
 *
 * <p>与 {@link SubjectVO} 的唯一差别是多一个 {@code status}：考生端接口只返回
 * {@code ENABLED}，那个字段在那边恒为一个常量，放进 {@code SubjectVO} 是噪音；
 * 而管理端必须能一眼看出哪一行已停用。
 *
 * <p>不做成"在 {@code SubjectVO} 上加一个可空 status"：一个"有时是 null、
 * 有时是 'ENABLED'"的字段，前端每个用到它的地方都得先判断自己在哪一端。
 *
 * <p>不带 {@code paperSummary}：列表页只回答"有哪些科目、哪个停用了"，
 * 卷面结构在详情接口里取。
 */
public record AdminSubjectVO(String id,
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
                             Integer sortNo,
                             String status) {
}
