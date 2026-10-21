package com.ruankao.service.question;

import com.ruankao.domain.dto.OutlineNodeRequest;
import com.ruankao.domain.dto.OutlineSortItem;
import com.ruankao.domain.vo.OutlineNodeVO;
import java.util.List;

/**
 * 大纲章节树（05 §5.4 第 10 条读、§5.10 第 41~45 条写）。
 *
 * <p>读与写放在同一个接口里：它们操作的是同一棵树的同一条不变量
 * （{@code level} 必须与 {@code parentId} 自洽），拆成两个类会让那条不变量
 * 的推导逻辑被两个类各用一半。
 *
 * <p>写方法都要求 {@code operatorId} 与 {@code ip}（AC-2/AC-3，见
 * {@link AdminSubjectService} 的说明）。
 */
public interface OutlineService {

    /**
     * 取章节树（05 §5.4 第 10 条）。
     *
     * @param includeDisabled 管理端 true（要能看见已停用章节），考生端 false
     * @throws com.ruankao.common.exception.BizException 3001：科目不存在；
     *         考生端（{@code includeDisabled = false}）还包括科目已停用
     */
    List<OutlineNodeVO> getTree(Long subjectId, boolean includeDisabled);

    /**
     * 新增章节（05 §5.10 第 42 条）。层级由 {@code parentId} 推导，深于 3 层返回 1001。
     *
     * @return 新章节 ID
     */
    String createNode(OutlineNodeRequest request, Long operatorId, String ip);

    /** 修改章节（05 §5.10 第 43 条）：改名、改父（含层级级联）、改排序号。 */
    void updateNode(Long nodeId, OutlineNodeRequest request, Long operatorId, String ip);

    /**
     * 删除章节（05 §5.10 第 44 条）。
     *
     * @throws com.ruankao.common.exception.BizException 1005：<b>有题目引用</b>，
     *         或<b>仍有子章节</b>（后一条是 05 未写明的补充，见实现类注释）
     * @throws com.ruankao.common.exception.BizException 1003：章节不存在
     */
    void deleteNode(Long nodeId, Long operatorId, String ip);

    /**
     * 批量排序（05 §5.10 第 45 条，请求体是整棵树的新顺序）。
     *
     * <p>全量覆盖式：提交的是"当前树应该长什么样"，不是增量。
     * 因此不传的节点保持原样，跨层拖拽（改 {@code parentId}）也在同一次提交里表达。
     */
    void sort(List<OutlineSortItem> items, Long operatorId, String ip);
}
