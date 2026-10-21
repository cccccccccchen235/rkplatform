package com.ruankao.service.question.impl;

import cn.hutool.core.lang.TypeReference;
import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.OutlineNodeRequest;
import com.ruankao.domain.dto.OutlineSortItem;
import com.ruankao.domain.entity.OutlineNode;
import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.OutlineNodeVO;
import com.ruankao.repository.cache.CacheDao;
import com.ruankao.repository.cache.CacheKeys;
import com.ruankao.repository.mapper.OutlineNodeMapper;
import com.ruankao.repository.mapper.SubjectMapper;
import com.ruankao.service.audit.OperationLogService;
import com.ruankao.service.question.OutlineConverter;
import com.ruankao.service.question.OutlineLevels;
import com.ruankao.service.question.OutlineService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutlineServiceImpl implements OutlineService {

    private static final String MODULE_SUBJECT = "SUBJECT";

    private static final String TARGET_TYPE_OUTLINE = "OUTLINE";

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String ORIGIN_MANUAL = "MANUAL";

    /** 根节点的 parentId 哨兵值（04 §4.5：0 为根，不是 null）。 */
    private static final long ROOT_PARENT_ID = 0L;

    private static final TypeReference<List<OutlineNodeVO>> TREE_TYPE = new TypeReference<>() {
    };

    private final OutlineNodeMapper outlineNodeMapper;
    private final SubjectMapper subjectMapper;
    private final CacheDao cacheDao;
    private final OperationLogService operationLogService;

    public OutlineServiceImpl(OutlineNodeMapper outlineNodeMapper,
                              SubjectMapper subjectMapper,
                              CacheDao cacheDao,
                              OperationLogService operationLogService) {
        this.outlineNodeMapper = outlineNodeMapper;
        this.subjectMapper = subjectMapper;
        this.cacheDao = cacheDao;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<OutlineNodeVO> getTree(Long subjectId, boolean includeDisabled) {
        Subject subject = subjectMapper.selectById(subjectId);
        // 考生端（includeDisabled = false）对已停用科目也报 3001，与科目详情同一口径：
        // 科目都下架了，它的大纲不该还能通过直链打开。
        if (subject == null || (!includeDisabled && !STATUS_ENABLED.equals(subject.getStatus()))) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }

        if (includeDisabled) {
            // 管理端不走缓存：编排中的树会被频繁读到，而缓存只会在每次改动时失效——
            // 一个"改一次、立刻再读"的循环里缓存命中率接近 0，只剩失效逻辑的复杂度。
            return OutlineConverter.toTree(outlineNodeMapper.selectBySubject(subjectId, true));
        }
        return cacheDao.getOrLoad(
                CacheKeys.outline(subjectId),
                TREE_TYPE,
                CacheDao.DEFAULT_TTL,
                () -> OutlineConverter.toTree(outlineNodeMapper.selectBySubject(subjectId, false)));
    }

    @Override
    @Transactional
    public String createNode(OutlineNodeRequest request, Long operatorId, String ip) {
        Long subjectId = parseId(request.getSubjectId(), "subjectId");
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }

        Long parentId = parseParentId(request.getParentId());
        int level;
        if (parentId == ROOT_PARENT_ID) {
            level = 1;
        }
        else {
            OutlineNode parent = outlineNodeMapper.selectById(parentId);
            if (parent == null || !subjectId.equals(parent.getSubjectId())) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "父章节不存在或不属于该科目：" + parentId);
            }
            level = parent.getLevel() + 1;
        }
        if (level > OutlineLevels.MAX_LEVEL) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "章节树最多 " + OutlineLevels.MAX_LEVEL + " 层，无法在第 " + level + " 层新增");
        }

        OutlineNode node = new OutlineNode();
        node.setSubjectId(subjectId);
        node.setParentId(parentId);
        node.setLevel(level);
        node.setName(request.getName().trim());
        // 未给排序号则排到同级末尾（与新增科目同一口径）
        node.setSortNo(request.getSortNo() != null
                ? request.getSortNo()
                : outlineNodeMapper.selectMaxSortNo(subjectId, parentId) + 1);
        node.setOrigin(request.getOrigin() == null ? ORIGIN_MANUAL : request.getOrigin());
        node.setStatus(STATUS_ENABLED);

        outlineNodeMapper.insert(node);

        cacheDao.evict(CacheKeys.outline(subjectId));
        operationLogService.record(operatorId, MODULE_SUBJECT, "CREATE", TARGET_TYPE_OUTLINE,
                node.getId(),
                "新增章节 " + node.getId() + "（科目 " + subjectId + "，第 " + level + " 层）" + node.getName(),
                ip);

        return String.valueOf(node.getId());
    }

    @Override
    @Transactional
    public void updateNode(Long nodeId, OutlineNodeRequest request, Long operatorId, String ip) {
        OutlineNode node = requireExisting(nodeId);
        Long subjectId = node.getSubjectId();

        // 请求体里的 subjectId 只用来对账，不作为写库依据：路径上的 {id} 已经确定了科目，
        // 若两者不一致，说明调用方拿着 A 科目的表单去改 B 科目的章节，宁可报错也不要按任一方写。
        if (request.getSubjectId() != null && !request.getSubjectId().isBlank()
                && !subjectId.equals(parseId(request.getSubjectId(), "subjectId"))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "章节不属于该科目：" + nodeId);
        }

        Long parentId = parseParentId(request.getParentId());

        // 在**整棵子树**上重算层级：只改一个节点的 level 是不够的，
        // 它下面的子孙都得跟着平移（见 OutlineLevels）。
        // 成环（把节点挂到自己的子孙下）会在这里被判出来。
        List<OutlineNode> all = outlineNodeMapper.selectBySubject(subjectId, true);
        Map<Long, Long> parentById = toParentMap(all);
        parentById.put(nodeId, parentId);
        Map<Long, Integer> levels = OutlineLevels.compute(parentById);

        int newLevel = levels.get(nodeId);
        if (newLevel > OutlineLevels.MAX_LEVEL) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "章节树最多 " + OutlineLevels.MAX_LEVEL + " 层，改父后该章节将处于第 " + newLevel + " 层");
        }

        OutlineNode updated = new OutlineNode();
        updated.setId(nodeId);
        updated.setParentId(parentId);
        updated.setLevel(newLevel);
        updated.setName(request.getName().trim());
        // 未提交的字段保持原值：这个接口也用于"只改名"，
        // 此时客户端不会带上排序号与出处，写成 null 会让 NOT NULL 列直接失败。
        updated.setSortNo(request.getSortNo() != null ? request.getSortNo() : node.getSortNo());
        updated.setOrigin(request.getOrigin() != null ? request.getOrigin() : node.getOrigin());
        outlineNodeMapper.update(updated);

        // 级联平移子孙的层级（只改 level，parent_id 与 sort_no 都没变）
        cascadeLevels(all, levels, nodeId);

        cacheDao.evict(CacheKeys.outline(subjectId));
        operationLogService.record(operatorId, MODULE_SUBJECT, "UPDATE", TARGET_TYPE_OUTLINE,
                nodeId,
                "修改章节 " + nodeId + "（科目 " + subjectId + "，第 " + newLevel + " 层）" + updated.getName(),
                ip);
    }

    @Override
    @Transactional
    public void deleteNode(Long nodeId, Long operatorId, String ip) {
        OutlineNode node = requireExisting(nodeId);

        int children = outlineNodeMapper.countChildren(nodeId);
        if (children > 0) {
            // 05 §5.10 第 44 条只写了"有题目引用时返回 1005"。这一条是补充：
            // 删掉一个有子章节的节点会把整棵子树变成永远查不到的孤儿
            // （它们仍按 level 排序取出来，但父链断了），所以同样用 1005 拒绝。
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                    "该章节下仍有 " + children + " 个子章节，请先删除子章节");
        }

        int refs = outlineNodeMapper.countQuestionRefs(nodeId);
        if (refs > 0) {
            // 不级联删除、不把题目的 outline_id 静默置空：题目挂了章节之后，
            // 删除章节等于让它的"章节路径"（QuestionVO.outlinePath）指向不存在的位置。
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                    "该章节被 " + refs + " 道题目引用，请先迁移题目后再删除");
        }

        outlineNodeMapper.deleteById(nodeId);

        cacheDao.evict(CacheKeys.outline(node.getSubjectId()));
        operationLogService.record(operatorId, MODULE_SUBJECT, "DELETE", TARGET_TYPE_OUTLINE,
                nodeId,
                "删除章节 " + nodeId + "（科目 " + node.getSubjectId() + "）" + node.getName(),
                ip);
    }

    @Override
    @Transactional
    public void sort(List<OutlineSortItem> items, Long operatorId, String ip) {
        if (items == null || items.isEmpty()) {
            // 一次提交里一个节点都没有，几乎只可能是客户端拼错了请求体；
            // 当成成功会让"排序没生效"变成一个没有回应的静默失败。
            throw new BizException(ErrorCode.PARAM_INVALID, "排序列表不能为空");
        }

        // 先把这批节点解析出来：顺带确定它们属于哪个科目。
        Map<Long, OutlineNode> touched = new LinkedHashMap<>();
        for (OutlineSortItem item : items) {
            if (item == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "排序列表中存在空项");
            }
            Long id = parseId(item.getId(), "id");
            // 显式判 sortNo：请求体是裸数组，元素级 @NotNull 是否被级联校验取决于
            // 校验器与容器注解的组合，不把"不写 NULL 进 NOT NULL 列"这件事
            // 押在一条可能不生效的注解上。
            if (item.getSortNo() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "排序号不能为空：id=" + id);
            }
            OutlineNode node = outlineNodeMapper.selectById(id);
            if (node == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "章节不存在：" + id);
            }
            touched.put(id, node);
        }
        Long subjectId = touched.values().iterator().next().getSubjectId();
        for (OutlineNode node : touched.values()) {
            if (!subjectId.equals(node.getSubjectId())) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "一次排序只能提交同一科目的章节，混入了科目 " + node.getSubjectId());
            }
        }

        // 以**库里现有的父链**为底，再叠加本次提交的新父子关系：
        // 提交里没提到的节点保持原位，因此"拖一个节点"与"重排整棵树"是同一条代码路径。
        List<OutlineNode> all = outlineNodeMapper.selectBySubject(subjectId, true);
        Map<Long, Long> parentById = toParentMap(all);
        Map<Long, Integer> sortNoById = new HashMap<>();
        for (OutlineSortItem item : items) {
            Long id = parseId(item.getId(), "id");
            parentById.put(id, parseParentId(item.getParentId()));
            sortNoById.put(id, item.getSortNo());
        }

        Map<Long, Integer> levels = OutlineLevels.compute(parentById);
        for (Map.Entry<Long, Integer> entry : levels.entrySet()) {
            if (entry.getValue() > OutlineLevels.MAX_LEVEL) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "章节树最多 " + OutlineLevels.MAX_LEVEL + " 层，节点 " + entry.getKey()
                                + " 将处于第 " + entry.getValue() + " 层");
            }
        }

        for (OutlineSortItem item : items) {
            Long id = parseId(item.getId(), "id");
            outlineNodeMapper.updateSort(id, parentById.get(id), sortNoById.get(id), levels.get(id));
        }
        // 跨层拖拽会让被拖节点的子孙层级整体平移。若客户端只提交了被拖的那个节点，
        // 上面这个循环覆盖不到它们——所以再按"重算值 ≠ 库中值"补一次。
        cascadeLevels(all, levels, null);

        cacheDao.evict(CacheKeys.outline(subjectId));
        operationLogService.record(operatorId, MODULE_SUBJECT, "UPDATE", TARGET_TYPE_OUTLINE,
                subjectId,
                "章节排序（科目 " + subjectId + "，本次提交 " + items.size() + " 项）",
                ip);
    }

    /**
     * 把"重算出的层级"与库中值不一致的节点同步过去。
     *
     * @param skipId 已完成主写入的节点（其 level 已随主写入落库），传 {@code null} 表示不跳过
     */
    private void cascadeLevels(List<OutlineNode> all, Map<Long, Integer> levels, Long skipId) {
        for (OutlineNode node : all) {
            if (skipId != null && skipId.equals(node.getId())) {
                continue;
            }
            Integer recomputed = levels.get(node.getId());
            if (recomputed != null && !recomputed.equals(node.getLevel())) {
                outlineNodeMapper.updateLevel(node.getId(), recomputed);
            }
        }
    }

    private OutlineNode requireExisting(Long nodeId) {
        OutlineNode node = outlineNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "章节不存在：" + nodeId);
        }
        return node;
    }

    private static Map<Long, Long> toParentMap(List<OutlineNode> nodes) {
        Map<Long, Long> parentById = new HashMap<>(nodes.size() * 2);
        for (OutlineNode node : nodes) {
            parentById.put(node.getId(), node.getParentId());
        }
        return parentById;
    }

    private static Long parseId(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, field + " 不能为空");
        }
        try {
            return Long.valueOf(raw.trim());
        }
        catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, field + " 不是合法 ID：" + raw);
        }
    }

    /** 空 / 缺省 / {@code "0"} 都表示根（04 §4.5：0 为根，不是 null）。 */
    private static Long parseParentId(String raw) {
        if (raw == null || raw.isBlank()) {
            return ROOT_PARENT_ID;
        }
        return parseId(raw, "parentId");
    }
}
