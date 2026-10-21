package com.ruankao.service.question;

import com.ruankao.domain.dto.KnowledgePointMergeRequest;
import com.ruankao.domain.dto.KnowledgePointRequest;
import com.ruankao.domain.vo.KnowledgePointVO;
import java.util.List;

/**
 * 知识点标签（05 §5.4 第 11 条读、§5.10 第 46~50 条写）。
 *
 * <p>标签是扁平的，没有层级（04 §4.5），所以本接口没有"树"的概念，
 * 也没有与 {@link OutlineService} 对称的层级校验。
 */
public interface KnowledgePointService {

    /**
     * 取某科目的标签（05 §5.4 第 11 条）。
     *
     * @param includeDisabled 管理端 true（05 §5.10 第 46 条要求含停用项），考生端 false
     * @throws com.ruankao.common.exception.BizException 3001：科目不存在；
     *         考生端还包括科目已停用
     */
    List<KnowledgePointVO> list(Long subjectId, boolean includeDisabled);

    /**
     * 新增标签（05 §5.10 第 47 条）。同科目内重名返回 1001。
     *
     * @return 新标签 ID
     */
    String create(KnowledgePointRequest request, Long operatorId, String ip);

    /** 修改标签（05 §5.10 第 48 条）：改名与改 {@code aliases}。 */
    void update(Long knowledgePointId, KnowledgePointRequest request, Long operatorId, String ip);

    /**
     * 删除标签（05 §5.10 第 49 条）。
     *
     * @throws com.ruankao.common.exception.BizException 1005：仍被题目引用
     */
    void delete(Long knowledgePointId, Long operatorId, String ip);

    /**
     * 合并标签（05 §5.10 第 50 条）：把 {@code sourceIds} 全部并入 {@code targetId}。
     *
     * <p>一次事务内完成"迁移题目关联 → 把来源的名字与同义词并入目标的 aliases → 删除来源标签"。
     * 合并后目标标签的题量按关联表重算（交集题不能被加两次）。
     */
    void merge(KnowledgePointMergeRequest request, Long operatorId, String ip);
}
