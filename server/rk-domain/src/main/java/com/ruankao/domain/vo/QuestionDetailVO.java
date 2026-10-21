package com.ruankao.domain.vo;

import java.util.List;

/**
 * 题目详情（05 §5.2「在 {@link QuestionVO} 基础上增加」答案相关字段）。
 *
 * <p><b>为什么平铺成一个 record 而不是组合</b>（如 {@code QuestionVO base}）：
 * 组合会把下发的 JSON 多嵌套一层 {@code base}，而契约要求这些字段与 {@code QuestionVO} 同层；
 * Java record 不能继承，无法"扩展"一个已有 record，故只能平铺。
 * <b>前 11 个字段的顺序与 {@link QuestionVO} 完全一致</b>，新增的答案字段排在最后，便于对照维护。
 *
 * <p>{@code correctAnswer}/{@code referenceAnswer}/{@code analysis}/{@code scorePoints}
 * 是客观/主观题的参考答案与解析，仅在允许查看答案的接口下发。
 *
 * <p><b>{@code isCorrect}/{@code userAnswer} 是"本次作答"的结果</b>：取卷
 * （{@code POST /practice/papers}）时二者恒为 {@code null}——那时用户还没作答。
 * {@code isCorrect} 为 {@code null} 也可能表示主观题（不自动判分），两种 {@code null} 的语义
 * 由 {@code questionType} 区分。
 */
public record QuestionDetailVO(String id,
                               String subjectId,
                               String outlineId,
                               List<String> outlinePath,
                               String questionType,
                               String stem,
                               List<QuestionImageVO> images,
                               Integer difficulty,
                               List<QuestionOptionVO> options,
                               List<QuestionKpVO> knowledgePoints,
                               boolean isFavorited,
                               String correctAnswer,
                               String referenceAnswer,
                               List<String> scorePoints,
                               String analysis,
                               Boolean isCorrect,
                               String userAnswer) {
}