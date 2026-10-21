package com.ruankao.domain.vo;

import java.util.List;

/**
 * 大纲章节节点（05 §5.2），树形递归。
 *
 * <p>{@code id} 与 {@code parentId} 都是 {@code String}：同 {@link SubjectVO} 的理由，
 * 主键为 {@code BIGINT UNSIGNED}，超过 2^53 前端 JS 会静默丢精度。
 *
 * <p><b>根节点的 {@code parentId} 恒为 {@code "0"}，不是 {@code null}</b>——这是 DDL
 * （{@code rk_outline_node.parent_id NOT NULL DEFAULT 0}）与 05 §5.2 共同的约定，
 * 前端按 {@code parentId === "0"} 找根。服务端若在转换时"顺手"把 0 变成 null，
 * 前端一个根节点都找不到，且这种错误在只有单层数据的科目上不会暴露。
 *
 * <p>{@code questionCount} 是<b>含子节点</b>的已发布题量（04 §4.5 的冗余列口径）。
 * 树已经递归构建好了，父节点不必让前端再递归求和——那会把一次遍历变成 N 次。
 *
 * @param children 子节点；<b>叶子节点为空列表而非 null</b>，省掉前端一次判空分支。
 */
public record OutlineNodeVO(String id,
                            String parentId,
                            Integer level,
                            String name,
                            Integer sortNo,
                            Integer questionCount,
                            List<OutlineNodeVO> children) {
}
