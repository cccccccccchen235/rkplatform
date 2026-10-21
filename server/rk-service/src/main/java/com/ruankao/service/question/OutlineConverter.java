package com.ruankao.service.question;

import com.ruankao.domain.entity.OutlineNode;
import com.ruankao.domain.vo.OutlineNodeVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 章节实体 → 树形 VO（03 §3.3.2：每个业务包内配一个 Converter）。
 *
 * <p><b>不用递归</b>：输入按 {@code level, sort_no, id} 升序（SQL 已排好），
 * 一趟扫描即可——层级小的先出现，所以处理某个节点时它的父节点<b>必然</b>已经在 Map 里
 * （除非父节点缺失，见下）。递归版本要额外写深度上限与环检测，而"按 level 排序后一趟扫"
 * 天然不会无限展开：环里的节点会有一端的父节点尚未入表，落到"父缺失"分支被当成根，
 * 环就此断开，而不是让请求栈溢出。
 */
public final class OutlineConverter {

    private static final Logger log = LoggerFactory.getLogger(OutlineConverter.class);

    private OutlineConverter() {
    }

    /**
     * 组装树，返回根节点数组。
     *
     * <p><b>父节点缺失的节点会被提升为根并记 WARN</b>，而不是丢弃。取舍理由：
     * 章节树是人工维护的数据，一条 {@code parent_id} 指向已删除节点的记录，
     * 如果只是悄悄藏起来，管理员看到的是"我明明建过的章节不见了"，
     * 且没有任何线索指向原因；提升为根至少让它出现在页面上，日志里也有它的 ID。
     * （与之相对，{@code SubjectServiceImpl} 对非法 {@code level} 是**跳过**——
     * 那里没有"根"这个合理归宿，一个 level=9 的科目放进哪个桶都是错的。）
     */
    public static List<OutlineNodeVO> toTree(List<OutlineNode> nodes) {
        Map<Long, OutlineNodeVO> byId = new HashMap<>(Math.max(16, nodes.size() * 2));
        List<OutlineNodeVO> roots = new ArrayList<>();

        for (OutlineNode node : nodes) {
            OutlineNodeVO vo = new OutlineNodeVO(
                    String.valueOf(node.getId()),
                    // 根节点的 parentId 恒为 "0"，不是 null（05 §5.2、04 §4.5）
                    String.valueOf(node.getParentId()),
                    node.getLevel(),
                    node.getName(),
                    node.getSortNo(),
                    node.getQuestionCount(),
                    // 叶子为空列表而非 null：省掉前端一次判空分支
                    new ArrayList<>());
            byId.put(node.getId(), vo);

            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(vo);
                continue;
            }
            OutlineNodeVO parent = byId.get(node.getParentId());
            if (parent == null) {
                log.warn("章节父节点缺失，已提升为根：id={}, subjectId={}, parentId={}",
                        node.getId(), node.getSubjectId(), node.getParentId());
                roots.add(vo);
                continue;
            }
            parent.children().add(vo);
        }

        // 同层内按 sortNo 排；level 的顺序由 SQL 保证，这里只兜平级
        Comparator<OutlineNodeVO> bySortNo =
                Comparator.comparing(OutlineNodeVO::sortNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OutlineNodeVO::id);
        roots.sort(bySortNo);
        byId.values().forEach(vo -> vo.children().sort(bySortNo));
        return roots;
    }
}
