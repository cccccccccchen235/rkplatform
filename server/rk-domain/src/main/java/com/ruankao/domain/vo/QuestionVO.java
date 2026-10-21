package com.ruankao.domain.vo;

import java.util.List;

/**
 * 练习/浏览场景的题目（05 §5.2）。字段即契约，改动需同步 05 与本类。
 *
 * <p><b>不含任何答案字段</b>（{@code correctAnswer}/{@code referenceAnswer}/{@code analysis}/
 * {@code scorePoints}）：取卷阶段就把答案下发给客户端，等于把判分口径交出去，客户端可以
 * 本地先判对错、跳过服务端复核（AC-3）。答案只在交卷/查看解析接口下发，见 {@link QuestionDetailVO}。
 *
 * <p><b>{@code isFavorited} 恒为 {@code boolean}</b>，游客为 {@code false}（非 {@code null}、
 * 非省略）：省掉前端"已登录才判断、未登录再显示第三种状态"的三态分支——未登录与未收藏
 * 在展示上没有区别。
 *
 * <p><b>{@code options} 仅客观题有</b>，主观题为 {@code null}——不要下发空列表，
 * 空列表会让前端误以为"这是一道没有选项的选择题"。
 *
 * <p>{@code outlinePath} 是章节<b>路径名</b>数组（从根到叶子），不是 ID 数组；
 * 面包屑与"这道题考哪一章"直接取用，无需前端再拼树。
 *
 * @param id 题目 ID（{@code String}，05 §5.1：ID 以字符串下发，避免 JS 精度丢失）
 * @param outlineId 叶子章节 ID
 * @param questionType SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/SUBJECTIVE/ESSAY
 * @param stem 题干，Markdown + KaTeX
 * @param difficulty 难度 1~5，1 最易
 */
public record QuestionVO(String id,
                         String subjectId,
                         String outlineId,
                         List<String> outlinePath,
                         String questionType,
                         String stem,
                         List<QuestionImageVO> images,
                         Integer difficulty,
                         List<QuestionOptionVO> options,
                         List<QuestionKpVO> knowledgePoints,
                         boolean isFavorited) {
}