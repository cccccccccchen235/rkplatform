package com.ruankao.service.question;

import com.ruankao.domain.dto.PaperTemplateSaveRequest;
import com.ruankao.domain.vo.PaperTemplateDetailVO;
import com.ruankao.domain.vo.PaperTemplateSaveResultVO;

/**
 * 管理端卷面模板配置（05 §5.10 第 51/52 条）。
 *
 * <p>读接口下发全字段（含 {@code perQuestionScore} / {@code durationMin/Max} /
 * {@code allowEarlySubmit}），写接口是<b>整体覆盖式</b>：一次提交把模板、场次、分节全部换掉。
 *
 * <p>写方法要求 {@code operatorId}（AC-3）与 {@code ip}（审计字段），
 * 与 {@link AdminSubjectService} 同一分工：Controller 从会话/请求头取值，Service 负责落审计。
 */
public interface PaperTemplateAdminService {

    /**
     * 读取某科目的卷面模板（含场次与分节）。
     *
     * <p>与考生端"没有启用模板返回 null"不同，管理端读不到时直接返回
     * {@code 4001}（该科目尚未配置试卷模板）——这个页面本来就是来配置它的，
     * 返回空对象会让"尚未配置"与"配置了 0 个场次"混在一起。
     *
     * @throws com.ruankao.common.exception.BizException 3001 科目不存在 / 4001 尚未配置模板
     */
    PaperTemplateDetailVO detail(Long subjectId);

    /**
     * 整体覆盖式保存（05 §5.10 第 52 条）。科目没有模板时<b>创建</b>，有则<b>替换</b>。
     *
     * <p>服务端强制 05 §5.10 的五条校验规则（序号连续、时长自洽、满分口径一致），
     * 违反返回 {@code 1001}；"配置题数超过已发布单选题量"只作为
     * {@code insufficientQuestions} 警告返回，不阻断保存。
     *
     * @throws com.ruankao.common.exception.BizException 1001 校验失败 / 3001 科目不存在
     */
    PaperTemplateSaveResultVO save(Long subjectId,
                                   PaperTemplateSaveRequest request,
                                   Long operatorId,
                                   String ip);
}
