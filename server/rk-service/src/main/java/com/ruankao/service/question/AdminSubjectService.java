package com.ruankao.service.question;

import com.ruankao.domain.dto.SubjectSaveRequest;
import com.ruankao.domain.vo.AdminSubjectVO;
import java.util.List;

/**
 * 管理端科目维护（05 §5.10 第 37~40 条）。
 *
 * <p>每个写方法都要三件事：校验、写库、<b>清缓存 + 记审计</b>。
 * 后两件是"改动科目"这件事不可分割的一部分——缓存不清则考生端一小时内看不到变化
 * （10 §10.4 的验收要求"1 秒内"），审计不记则 AC-2 形同虚设。
 * 因此它们在这里实现，而不是留给 Controller 记得去调。
 *
 * <p>写方法都要求 {@code operatorId}（AC-3：用户数据访问必须携带 user_id）
 * 与 {@code ip}（审计字段）。{@code ip} 由 rk-api 层从请求里取，
 * 业务层不感知 HTTP（06 §6.1）。
 */
public interface AdminSubjectService {

    /** 管理端列表（05 §5.10 第 37 条）：<b>含已停用</b>。 */
    List<AdminSubjectVO> listForAdmin();

    /**
     * 新增科目（05 §5.10 第 38 条）。
     *
     * <p>新科目一律为 {@code ENABLED}：刚建完就藏起来不是任何人的意图。
     * 排序号取同级最大 + 1，排在同级末尾。
     *
     * @return 新科目 ID
     * @throws com.ruankao.common.exception.BizException 1001 取值非法 / 代码重复
     */
    String create(SubjectSaveRequest request, Long operatorId, String ip);

    /**
     * 修改科目（05 §5.10 第 39 条）。{@code level} 被忽略（05 §5.10 第 38 条的说明），
     * {@code sortNo} 保持原值——排序不通过这个接口改。
     *
     * @throws com.ruankao.common.exception.BizException 1001 / 3001
     */
    void update(Long subjectId, SubjectSaveRequest request, Long operatorId, String ip);

    /**
     * 启用 / 停用（05 §5.10 第 40 条）。<b>停用即下架</b>，考生端随即不可见。
     *
     * <p>状态没有实际变化时直接返回：不写库、不记审计。记一条"把启用改成启用"
     * 只会稀释审计日志的信噪比。
     *
     * @throws com.ruankao.common.exception.BizException 1001 取值非法 / 3001 科目不存在
     */
    void changeStatus(Long subjectId, String status, Long operatorId, String ip);
}
