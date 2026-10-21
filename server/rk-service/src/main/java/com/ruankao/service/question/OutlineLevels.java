package com.ruankao.service.question;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 由 {@code parentId} 推导层级——章节树唯一允许的 level 来源。
 *
 * <p>层级是<b>推导量</b>而非输入量：只要"子节点的 level = 父节点 level + 1"这一条成立，
 * {@code level} 就完全由 {@code parentId} 决定。让客户端提交 level（或在多处各推导一次）
 * 迟早会出现"level=3 却挂在 level=1 下"的行——而它的表现是树渲染错位，
 * 排查时得先怀疑数据再怀疑代码。
 *
 * <p>算法是带记忆化的 DFS：入参是一张 {@code id → parentId} 的完整映射，
 * 对每个节点沿父链上溯直到根（{@code parentId = 0}）。上溯路径用 {@code path} 记录，
 * <b>检出环时抛 1001 而不是栈溢出</b>——环只可能来自人工改库或并发拖拽写坏的数据，
 * 让请求以"参数校验失败"结束，比让线程栈炸掉然后没人知道是哪个节点的问题要好。
 *
 * <p>必须是 {@code public}：实现类在 {@code question.impl} 子包内，包私有访问不到
 * （与 {@link SubjectConverter} 同一条约束）。它服务于大纲的两个写入口
 * （单节点编辑、批量排序），不构成对外契约，故除了这两个入口外没人该用它。
 */
public final class OutlineLevels {

    /** 章节树的最大深度（10 §10.4：3 层）。 */
    public static final int MAX_LEVEL = 3;

    private OutlineLevels() {
    }

    /**
     * 推导每个节点的层级。
     *
     * @param parentById 完整的 {@code id → parentId} 映射；根节点的 parentId 用
     *                   {@code 0}（或 {@code null}）表示
     * @return {@code id → level}，与入参同键集
     * @throws BizException 1001：父节点不在映射内，或父子关系成环
     */
    public static Map<Long, Integer> compute(Map<Long, Long> parentById) {
        Map<Long, Integer> levels = new HashMap<>(parentById.size() * 2);
        for (Long id : parentById.keySet()) {
            levelOf(id, parentById, levels, new HashSet<>());
        }
        return levels;
    }

    private static int levelOf(Long id,
                               Map<Long, Long> parentById,
                               Map<Long, Integer> memo,
                               Set<Long> path) {
        Integer cached = memo.get(id);
        if (cached != null) {
            return cached;
        }
        if (!path.add(id)) {
            // 回到自己 = 环。此时 path 里就是环上的节点，报出来让排查有起点。
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "章节父子关系成环，涉及节点：" + path);
        }

        Long parentId = parentById.get(id);
        int level;
        if (parentId == null || parentId == 0L) {
            level = 1;
        }
        else if (!parentById.containsKey(parentId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "父章节不存在：" + parentId);
        }
        else {
            level = levelOf(parentId, parentById, memo, path) + 1;
        }

        // 回溯时出栈：path 表示的是"当前这条上溯链"，不是"访问过的所有节点"
        path.remove(id);
        memo.put(id, level);
        return level;
    }
}
