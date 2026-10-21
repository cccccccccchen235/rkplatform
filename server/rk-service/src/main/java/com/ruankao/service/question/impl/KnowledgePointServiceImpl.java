package com.ruankao.service.question.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.KnowledgePointMergeRequest;
import com.ruankao.domain.dto.KnowledgePointRequest;
import com.ruankao.domain.entity.KnowledgePoint;
import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.KnowledgePointVO;
import com.ruankao.repository.mapper.KnowledgePointMapper;
import com.ruankao.repository.mapper.SubjectMapper;
import com.ruankao.service.audit.OperationLogService;
import com.ruankao.service.question.KnowledgePointConverter;
import com.ruankao.service.question.KnowledgePointService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgePointServiceImpl.class);

    private static final String MODULE_SUBJECT = "SUBJECT";

    private static final String TARGET_TYPE_KNOWLEDGE_POINT = "KNOWLEDGE_POINT";

    private static final String STATUS_ENABLED = "ENABLED";

    /** {@code aliases} 的列宽（04 §4.5：VARCHAR(255)）。 */
    private static final int ALIASES_MAX_LENGTH = 255;

    private final KnowledgePointMapper knowledgePointMapper;
    private final SubjectMapper subjectMapper;
    private final OperationLogService operationLogService;

    public KnowledgePointServiceImpl(KnowledgePointMapper knowledgePointMapper,
                                     SubjectMapper subjectMapper,
                                     OperationLogService operationLogService) {
        this.knowledgePointMapper = knowledgePointMapper;
        this.subjectMapper = subjectMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<KnowledgePointVO> list(Long subjectId, boolean includeDisabled) {
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null || (!includeDisabled && !STATUS_ENABLED.equals(subject.getStatus()))) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }
        return KnowledgePointConverter.toVOList(
                knowledgePointMapper.selectBySubject(subjectId, includeDisabled));
    }

    @Override
    @Transactional
    public String create(KnowledgePointRequest request, Long operatorId, String ip) {
        Long subjectId = parseId(request.subjectId(), "subjectId");
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }

        String name = request.name().trim();
        requireNameAvailable(subjectId, name, null);

        KnowledgePoint entity = new KnowledgePoint();
        entity.setSubjectId(subjectId);
        entity.setName(name);
        entity.setAliases(normalizeAliases(request.aliases()));
        entity.setStatus(STATUS_ENABLED);
        knowledgePointMapper.insert(entity);

        operationLogService.record(operatorId, MODULE_SUBJECT, "CREATE", TARGET_TYPE_KNOWLEDGE_POINT,
                entity.getId(), "新增知识点 " + entity.getId() + "（科目 " + subjectId + "）" + name, ip);

        return String.valueOf(entity.getId());
    }

    @Override
    @Transactional
    public void update(Long knowledgePointId, KnowledgePointRequest request, Long operatorId, String ip) {
        KnowledgePoint existing = requireExisting(knowledgePointId);
        Long subjectId = existing.getSubjectId();

        if (request.subjectId() != null && !request.subjectId().isBlank()
                && !subjectId.equals(parseId(request.subjectId(), "subjectId"))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "知识点不属于该科目：" + knowledgePointId);
        }

        String name = request.name().trim();
        // 排除自己：改名时"改成自己原来的名字"不该被判成重复
        requireNameAvailable(subjectId, name, knowledgePointId);

        KnowledgePoint entity = new KnowledgePoint();
        entity.setId(knowledgePointId);
        entity.setName(name);
        entity.setAliases(normalizeAliases(request.aliases()));
        knowledgePointMapper.update(entity);

        operationLogService.record(operatorId, MODULE_SUBJECT, "UPDATE", TARGET_TYPE_KNOWLEDGE_POINT,
                knowledgePointId, "修改知识点 " + knowledgePointId + "（科目 " + subjectId + "）" + name, ip);
    }

    @Override
    @Transactional
    public void delete(Long knowledgePointId, Long operatorId, String ip) {
        KnowledgePoint existing = requireExisting(knowledgePointId);

        int refs = knowledgePointMapper.countQuestionRefs(knowledgePointId);
        if (refs > 0) {
            // 与章节删除同一口径（05 §5.10 第 49 条）：有引用就拒绝，
            // 让管理员先把题目迁走或改用"合并"，而不是让那些题目的知识点凭空消失。
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                    "该知识点被 " + refs + " 道题目引用，请改用合并或先解除关联");
        }

        knowledgePointMapper.deleteById(knowledgePointId);

        operationLogService.record(operatorId, MODULE_SUBJECT, "DELETE", TARGET_TYPE_KNOWLEDGE_POINT,
                knowledgePointId,
                "删除知识点 " + knowledgePointId + "（科目 " + existing.getSubjectId() + "）" + existing.getName(),
                ip);
    }

    @Override
    @Transactional
    public void merge(KnowledgePointMergeRequest request, Long operatorId, String ip) {
        Long targetId = parseId(request.targetId(), "targetId");
        KnowledgePoint target = requireExisting(targetId);
        Long subjectId = target.getSubjectId();

        List<KnowledgePoint> sources = new ArrayList<>(request.sourceIds().size());
        for (String raw : request.sourceIds()) {
            Long sourceId = parseId(raw, "sourceIds");
            if (sourceId.equals(targetId)) {
                // "把自己合并进自己"会走成"先删自己再往里迁"的删除语义——
                // 这是一次静默的数据丢失，必须挡住。
                throw new BizException(ErrorCode.PARAM_INVALID, "来源中不能包含目标知识点：" + targetId);
            }
            KnowledgePoint source = requireExisting(sourceId);
            if (!subjectId.equals(source.getSubjectId())) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "不能跨科目合并：知识点 " + sourceId + " 属于科目 " + source.getSubjectId());
            }
            sources.add(source);
        }

        StringBuilder mergedNames = new StringBuilder();
        for (KnowledgePoint source : sources) {
            // 先并关联（INSERT IGNORE 跳过与目标重复的题），再清源标签的残留关联行
            knowledgePointMapper.migrateQuestionRefs(source.getId(), targetId);
            knowledgePointMapper.deleteQuestionRefsByKp(source.getId());
            knowledgePointMapper.deleteById(source.getId());
            mergedNames.append(source.getName()).append(' ');
        }

        // 来源标签的名字与同义词并入目标：合并之后，按旧名字检索应该仍然命中目标，
        // 否则"合并"等于把那些检索入口弄丢了。
        String mergedAliases = mergeAliases(target, sources);
        KnowledgePoint updated = new KnowledgePoint();
        updated.setId(targetId);
        updated.setName(target.getName());
        updated.setAliases(mergedAliases);
        knowledgePointMapper.update(updated);

        // 题量按关联表重算：交集题会被两个标签同时引用，直接相加会重复计数
        knowledgePointMapper.refreshQuestionCount(targetId);

        operationLogService.record(operatorId, MODULE_SUBJECT, "UPDATE", TARGET_TYPE_KNOWLEDGE_POINT,
                targetId,
                "合并知识点 " + request.sourceIds() + " → " + targetId + "（" + mergedNames.toString().trim() + "）",
                ip);
    }

    /**
     * 名称唯一性检查（同科目内，对应库上的 {@code uk_subject_name}）。
     *
     * <p>库才是唯一裁决者，这里只是"提前给出更快的答案"：并发插入仍会抛
     * {@code DuplicateKeyException}，落到兜底分支变成 1000——那是一次真正的竞态，
     * 用 1000 表示"系统内部错误"比伪装成参数错误更诚实。
     */
    private void requireNameAvailable(Long subjectId, String name, Long excludeId) {
        KnowledgePoint existing = knowledgePointMapper.selectBySubjectAndName(subjectId, name);
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该科目下已存在同名知识点：" + name);
        }
    }

    /**
     * 把来源标签的名字与同义词并入目标的 {@code aliases}。
     *
     * <p>超出列宽时<b>截断并记 WARN</b>（列出被丢掉的名字），而不是让整个合并失败：
     * 合并的首要目的是迁移题目关联，为了一个"同义词装不下"把这件事挡住是本末倒置。
     * 被丢掉的名字会出现在日志里，需要时可以手工补进 aliases。
     */
    private static String mergeAliases(KnowledgePoint target, List<KnowledgePoint> sources) {
        // LinkedHashSet：去重且保持"目标原有 → 各来源依次追加"的顺序，结果可复现
        Set<String> aliases = new LinkedHashSet<>(splitAliases(target.getAliases()));
        for (KnowledgePoint source : sources) {
            aliases.add(source.getName());
            aliases.addAll(splitAliases(source.getAliases()));
        }
        // 目标自己的名字不该出现在自己的同义词里
        aliases.remove(target.getName());

        StringBuilder merged = new StringBuilder();
        List<String> dropped = new ArrayList<>();
        for (String alias : aliases) {
            int extra = (merged.length() == 0 ? 0 : 2) + alias.length();
            if (merged.length() + extra > ALIASES_MAX_LENGTH) {
                dropped.add(alias);
                continue;
            }
            if (merged.length() > 0) {
                merged.append(", ");
            }
            merged.append(alias);
        }
        if (!dropped.isEmpty()) {
            log.warn("合并时同义词超出 {} 字符被丢弃：target={}, dropped={}",
                    ALIASES_MAX_LENGTH, target.getId(), dropped);
        }
        return merged.length() == 0 ? null : merged.toString();
    }

    private static List<String> splitAliases(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    /** 落库前把 aliases 归一化成"逗号 + 空格"分隔，避免同一个标签在库里出现多种写法。 */
    private static String normalizeAliases(String raw) {
        List<String> parts = splitAliases(raw);
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private KnowledgePoint requireExisting(Long knowledgePointId) {
        KnowledgePoint entity = knowledgePointMapper.selectById(knowledgePointId);
        if (entity == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "知识点不存在：" + knowledgePointId);
        }
        return entity;
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
}
