package com.ruankao.service.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruankao.domain.entity.OutlineNode;
import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.StatOutlineNodeVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 章节掌握度树的单测（05 §5.8 第 34 条、10 §10.8 的"父节点为子节点题量加权"）。
 */
class StatOutlineTreeBuilderTest {

    private static final double PRIOR = 0.5;

    @Test
    @DisplayName("空的科目大纲返回空列表")
    void emptyNodesGiveEmptyTree() {
        assertTrue(StatOutlineTreeBuilder.build(List.of(), Map.of(), Map.of(), PRIOR).isEmpty());
    }

    @Test
    @DisplayName("父节点掌握度 = 子节点按其题量加权；未练过的叶子取先验")
    void parentMasteryIsWeightedByChildQuestionCount() {
        // 1 ─┬─ 2 ── 4（练过，10 题全对）
        //    └─ 3（未练，30 题）
        List<OutlineNode> nodes = List.of(
                node(1L, 0L, 1, 1),
                node(2L, 1L, 2, 1),
                node(3L, 1L, 2, 2),
                node(4L, 2L, 3, 1));

        Map<Long, Integer> published = Map.of(4L, 10, 3L, 30);
        Map<Long, StatUserOutline> stats = Map.of(4L, outlineStat(4L, 10, 10, "1.0000", "1.0000"));

        List<StatOutlineNodeVO> roots = StatOutlineTreeBuilder.build(nodes, published, stats, PRIOR);

        assertEquals(1, roots.size());
        StatOutlineNodeVO root = roots.get(0);

        // 根：Σ(1.0×10 + 0.5×30) / 40 = 0.625
        assertEquals(0, new BigDecimal("0.6250").compareTo(root.mastery()));
        assertEquals(10, root.answeredCount());
        assertEquals(40, root.questionCount());
        assertEquals(0, new BigDecimal("1.0000").compareTo(root.accuracy()));

        StatOutlineNodeVO node2 = root.children().get(0);
        StatOutlineNodeVO node3 = root.children().get(1);
        // 叶子 4 的掌握度沿树上浮到中间节点 2
        assertEquals(0, new BigDecimal("1.0000").compareTo(node2.mastery()));
        assertEquals(10, node2.questionCount());
        // 未练过的 3 取先验
        assertEquals(0, new BigDecimal("0.5000").compareTo(node3.mastery()));
        assertEquals(0, node3.answeredCount());
        // 根节点 parentId 恒为 "0"
        assertEquals("0", root.parentId());
    }

    @Test
    @DisplayName("没有任何已发布题的章节取先验，不除零")
    void chaptersWithoutQuestionsFallBackToPrior() {
        List<OutlineNode> nodes = List.of(node(1L, 0L, 1, 1), node(2L, 1L, 2, 1));

        List<StatOutlineNodeVO> roots = StatOutlineTreeBuilder.build(nodes, Map.of(), Map.of(), PRIOR);

        assertEquals(1, roots.size());
        assertEquals(0, new BigDecimal("0.5000").compareTo(roots.get(0).mastery()));
        assertEquals(0, new BigDecimal("0.5000").compareTo(roots.get(0).children().get(0).mastery()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(roots.get(0).accuracy()));
    }

    private static OutlineNode node(Long id, Long parentId, int level, int sortNo) {
        OutlineNode node = new OutlineNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setLevel(level);
        node.setSortNo(sortNo);
        node.setName("节点" + id);
        return node;
    }

    private static StatUserOutline outlineStat(Long outlineId, int answered, int correct,
                                               String accuracy, String mastery) {
        StatUserOutline row = new StatUserOutline();
        row.setOutlineId(outlineId);
        row.setAnsweredCount(answered);
        row.setCorrectCount(correct);
        row.setAccuracy(new BigDecimal(accuracy));
        row.setMastery(new BigDecimal(mastery));
        return row;
    }
}
