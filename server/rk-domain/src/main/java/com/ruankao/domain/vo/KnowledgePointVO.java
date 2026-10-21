package com.ruankao.domain.vo;

/**
 * 知识点标签（05 §5.2）。
 *
 * <p>{@code aliases} 是<b>逗号分隔的单个字符串</b>，不是数组——库里
 * （{@code rk_knowledge_point.aliases VARCHAR(255)}）就是一列，05 §5.2 也这么定。
 * 拆成 {@code List<String>} 会在两个方向各做一次解析，而调用方只做整串展示
 * 或 {@code LIKE} 检索，拿不到收益。
 *
 * <p>{@code status} 只有管理端列表会返回 {@code DISABLED}；公开接口
 * （05 §5.4 第 11 条）只返回 {@code ENABLED}，过滤在服务层完成，不靠前端。
 */
public record KnowledgePointVO(String id,
                               String subjectId,
                               String name,
                               String aliases,
                               Integer questionCount,
                               String status) {
}
