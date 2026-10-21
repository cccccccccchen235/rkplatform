package com.ruankao.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 章节掌握度树（05 §5.8 第 34 条）。
 *
 * <p>与公开大纲的 {@link OutlineNodeVO} 字段几乎相同，但多三个统计量，因此独立成形而不是
 * 给 {@link OutlineNodeVO} 加三个可空字段：那样会让公开接口
 * {@code GET /subjects/{id}/outline} 的每个节点都多出三个恒为 {@code null} 的键
 * （Jackson 配置是 {@code always}，null 也会下发），把一个"统计专用"的形状泄漏进公开契约。
 *
 * <p>{@code questionCount} 是**本节点含子节点**的已发布题量，由 {@code rk_question} 现算
 * （不取 {@code rk_outline_node.question_count} 冗余列）——父节点掌握度的权重就是它，
 * 权重必须与题量的口径同源，否则"父节点按子节点题量加权"这句话会算在一个可能过期的数字上。
 *
 * <p>父节点的 {@code mastery} = 其直接子节点按 {@code questionCount} 加权平均；
 * 叶子节点用自己的掌握度。无题的节点取先验。
 *
 * @param children 子节点；叶子为空列表而非 null
 */
public record StatOutlineNodeVO(
        String id,
        String parentId,
        Integer level,
        String name,
        Integer sortNo,
        Integer questionCount,
        Integer answeredCount,
        BigDecimal accuracy,
        BigDecimal mastery,
        List<StatOutlineNodeVO> children) {
}
