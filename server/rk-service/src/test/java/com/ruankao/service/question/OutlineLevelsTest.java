package com.ruankao.service.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 章节层级推导（{@link OutlineLevels}）。
 *
 * <p>纯逻辑，不起 Spring 容器、不连库——这条规则的价值就在于它是纯函数。
 */
class OutlineLevelsTest {

    private static Map<Long, Long> parents(Object... idParentPairs) {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (int i = 0; i < idParentPairs.length; i += 2) {
            map.put((Long) idParentPairs[i], (Long) idParentPairs[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("根为 1 级，子为父 + 1")
    void derivesLevelFromParentChain() {
        Map<Long, Integer> levels = OutlineLevels.compute(
                parents(1L, 0L,   // 根
                        2L, 1L,   // 一级的孩子 → 2 级
                        3L, 2L,   // 二级的孩子 → 3 级
                        4L, 0L)); // 另一个根

        assertEquals(1, levels.get(1L));
        assertEquals(2, levels.get(2L));
        assertEquals(3, levels.get(3L));
        assertEquals(1, levels.get(4L));
    }

    @Test
    @DisplayName("parentId 为 null 也当作根（DDL 用 0，但实体允许未加载该列）")
    void nullParentCountsAsRoot() {
        Map<Long, Integer> levels = OutlineLevels.compute(parents(7L, null));
        assertEquals(1, levels.get(7L));
    }

    @Test
    @DisplayName("父节点不在映射内 → 1001，而不是当成根静默放过")
    void missingParentIsRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> OutlineLevels.compute(parents(5L, 999L)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("999"), "提示里要带上缺失的父节点 ID：" + ex.getMessage());
    }

    @Test
    @DisplayName("自引用（自己是自己的父）→ 1001")
    void selfReferenceIsRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> OutlineLevels.compute(parents(5L, 5L)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("两节点成环 → 1001，不栈溢出")
    void twoNodeCycleIsRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> OutlineLevels.compute(parents(5L, 6L, 6L, 5L)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("成环"), ex.getMessage());
    }

    @Test
    @DisplayName("三节点成环也能被检出（环不一定经过 DFS 的起点）")
    void threeNodeCycleIsRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> OutlineLevels.compute(parents(1L, 3L, 2L, 1L, 3L, 2L)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("共用同一个父的兄弟各自独立推导，记忆化不串味")
    void siblingsShareLevelWithoutCrossTalk() {
        Map<Long, Integer> levels = OutlineLevels.compute(
                parents(1L, 0L, 2L, 1L, 3L, 1L, 4L, 2L, 5L, 3L));
        assertEquals(2, levels.get(2L));
        assertEquals(2, levels.get(3L));
        assertEquals(3, levels.get(4L), "2 的孩子应是 3 级");
        assertEquals(3, levels.get(5L), "3 的孩子应是 3 级");
    }
}
