package com.ruankao.service.stat;

import com.ruankao.domain.entity.OutlineNode;
import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.StatOutlineNodeVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节掌握度树：把"叶子章节的统计行"沿树向上滚成每个节点的掌握度（05 §5.8 第 34 条、
 * 10 §10.8 的验收项"父节点为其子节点的题量加权"）。
 *
 * <p>纯函数、无 Spring 依赖：父节点加权是本轮最容易"看起来对、其实错"的一段
 * （权重取错一层、忘记先验、空章节除零都不会抛异常），值得单测钉死。
 *
 * <p>两个刻意的口径：
 * <ul>
 *   <li>{@code questionCount} 由 {@code rk_question} 现算（子树已发布题量），不取
 *       {@code rk_outline_node.question_count} 冗余列——权重必须与题量同源。</li>
 *   <li>没练过 / 没题的节点掌握度取<b>先验</b>（默认 0.5），与 06 §6.10.1 的
 *       {@code a = 0 → prior} 一致；显示成 0 会让"没学过"看起来像"学废了"。</li>
 * </ul>
 */
public final class StatOutlineTreeBuilder {

    private static final int SCALE = 4;

    private StatOutlineTreeBuilder() {
    }

    /**
     * @param nodes          某科目的全部章节节点（{@code level, sort_no, id} 升序，见 OutlineNodeMapper）
     * @param publishedCounts 叶子章节 → 已发布题量
     * @param statRows        已练过的章节 → 统计行
     * @param prior           先验掌握度
     * @return 根节点数组；无节点时返回空列表
     */
    public static List<StatOutlineNodeVO> build(List<OutlineNode> nodes,
                                                Map<Long, Integer> publishedCounts,
                                                Map<Long, StatUserOutline> statRows,
                                                double prior) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        Map<Long, Holder> byId = new HashMap<>(nodes.size() * 2);
        List<Holder> roots = new ArrayList<>();
        for (OutlineNode node : nodes) {
            byId.put(node.getId(), new Holder(node));
        }
        for (OutlineNode node : nodes) {
            Holder holder = byId.get(node.getId());
            Long parentId = node.getParentId();
            Holder parent = parentId == null || parentId == 0L ? null : byId.get(parentId);
            if (parent == null) {
                roots.add(holder);
            } else {
                parent.children.add(holder);
            }
        }

        Comparator<Holder> bySort = Comparator
                .comparing((Holder h) -> h.node.getSortNo(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(h -> h.node.getId());
        roots.sort(bySort);
        byId.values().forEach(h -> h.children.sort(bySort));

        List<StatOutlineNodeVO> result = new ArrayList<>(roots.size());
        for (Holder root : roots) {
            compute(root, publishedCounts, statRows, prior);
            result.add(toVO(root));
        }
        return result;
    }

    /** 后序计算：先算子节点，父节点才能拿到它们的题量与掌握度。 */
    private static void compute(Holder holder,
                                Map<Long, Integer> publishedCounts,
                                Map<Long, StatUserOutline> statRows,
                                double prior) {
        long id = holder.node.getId();
        if (holder.children.isEmpty()) {
            StatUserOutline row = statRows.get(id);
            holder.answered = row == null || row.getAnsweredCount() == null ? 0 : row.getAnsweredCount();
            holder.correct = row == null || row.getCorrectCount() == null ? 0 : row.getCorrectCount();
            holder.weight = Math.max(0, publishedCounts.getOrDefault(id, 0));
            holder.mastery = row == null || row.getMastery() == null
                    ? round4(prior)
                    : row.getMastery();
            holder.accuracy = ratio(holder.correct, holder.answered);
            return;
        }

        int answered = 0;
        int correct = 0;
        int weight = 0;
        double weightedMastery = 0.0;
        for (Holder child : holder.children) {
            compute(child, publishedCounts, statRows, prior);
            answered += child.answered;
            correct += child.correct;
            weight += child.weight;
            weightedMastery += child.mastery.doubleValue() * child.weight;
        }
        holder.answered = answered;
        holder.correct = correct;
        holder.weight = weight;
        holder.accuracy = ratio(correct, answered);
        holder.mastery = weight > 0 ? round4(weightedMastery / weight) : round4(prior);
    }

    private static StatOutlineNodeVO toVO(Holder holder) {
        List<StatOutlineNodeVO> children = new ArrayList<>(holder.children.size());
        for (Holder child : holder.children) {
            children.add(toVO(child));
        }
        return new StatOutlineNodeVO(
                String.valueOf(holder.node.getId()),
                String.valueOf(holder.node.getParentId() == null ? 0L : holder.node.getParentId()),
                holder.node.getLevel(),
                holder.node.getName(),
                holder.node.getSortNo(),
                holder.weight,
                holder.answered,
                holder.accuracy,
                holder.mastery,
                children);
    }

    private static BigDecimal ratio(int correct, int answered) {
        if (answered <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(correct)
                .divide(BigDecimal.valueOf(answered), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal round4(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 中间状态。可变类 + 后序计算，避免在递归里反复查找 Map。 */
    private static final class Holder {
        private final OutlineNode node;
        private final List<Holder> children = new ArrayList<>();
        private int answered;
        private int correct;
        private int weight;
        private BigDecimal accuracy = BigDecimal.ZERO;
        private BigDecimal mastery = BigDecimal.ZERO;

        private Holder(OutlineNode node) {
            this.node = node;
        }
    }
}
