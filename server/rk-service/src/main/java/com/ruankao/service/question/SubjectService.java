package com.ruankao.service.question;

import com.ruankao.domain.vo.SubjectVO;
import java.util.List;
import java.util.Map;

/**
 * 科目查询（05 §5.4）。
 *
 * <p>放在 {@code question} 业务包而非新建 {@code subject} 包：03 §3.3.2 定下的六个业务包
 * （question / practice / exam / stats / review / user）里，科目、大纲、知识点都是"取数"，
 * 属于 question 包的"题目查询、组卷取数"职责。
 *
 * <p>本接口只读。<b>写</b>在 {@link AdminSubjectService}——读与写的规则差别很大
 * （读要缓存、要按级别分组；写要校验重名、要记审计、要清缓存），
 * 挤进一个接口会让每个实现方法都要先判断自己是哪一类。
 */
public interface SubjectService {

    /**
     * 按级别分组查询科目。
     *
     * <p><b>走缓存</b>（{@code rk:cache:subject:list}，06 §6.11）：缓存里存的是
     * "不带过滤条件的全量分组结果"，过滤在内存里做。理由是这个键只有一份
     * （按 06 §6.11，管理端改动科目时 DEL 的就是它），若按 {@code level}+
     * {@code onlyConfigured} 组合再拆成多个键，"改科目要清哪些键"就变成了一个
     * 需要枚举的组合问题——漏清一个组合的表现是"某个筛选下看到的还是旧数据"，
     * 极难复现。
     *
     * @param level          级别过滤，null/空表示不过滤
     * @param onlyConfigured 仅返回已配置启用中卷面模板的科目
     * @return key 为 {@link com.ruankao.domain.enums.SubjectLevel} 的三个取值，
     * <b>三个 key 恒存在</b>（空组为 {@code []}），值为该级别科目、按 {@code sort_no} 升序
     */
    Map<String, List<SubjectVO>> listGroupedByLevel(String level, boolean onlyConfigured);

    /**
     * 科目详情（05 §5.4 第 9 条）——<b>唯一填充 {@code paperSummary} 的接口</b>。
     *
     * @throws com.ruankao.common.exception.BizException 3001：科目不存在<b>或</b>已停用
     *         （05 §5.1.2 把这两种情况合并成同一个码）
     */
    SubjectVO getDetail(Long subjectId);
}
