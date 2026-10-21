package com.ruankao.service.exam;

import com.ruankao.common.api.PageResult;
import com.ruankao.domain.dto.ExamAnswerRequest;
import com.ruankao.domain.vo.ExamAnswerResultVO;
import com.ruankao.domain.vo.ExamHistoryVO;
import com.ruankao.domain.vo.ExamResultVO;
import com.ruankao.domain.vo.ExamSectionAdvanceVO;
import com.ruankao.domain.vo.ExamSessionVO;
import com.ruankao.domain.vo.ExamSittingFinishVO;
import com.ruankao.domain.vo.ExamSittingStartVO;

/**
 * 模拟机考闭环（05 §5.6 第 16~25 条）。
 *
 * <p>计时真值由 {@code exam_session_sitting.started_at} 派生（{@link ExamTiming}），
 * 任何入口进入时都会先做一次**惰性超时结算**（06 §6.9.2）——这是"超时后任意接口访问
 * 触发自动交卷"这条验收项的落点。
 */
public interface ExamService {

    /** 发起模拟考（组卷 + 快照 + 4003/4001/4002 校验）。 */
    ExamSessionVO create(Long userId, Long subjectId);

    /** 历史成绩（默认不含 ABANDONED，永不含 IN_PROGRESS）。 */
    PageResult<ExamHistoryVO> history(Long userId, Long subjectId, boolean includeAbandoned,
                                      int pageNum, int pageSize);

    /** 会话详情；断点续答靠它恢复全部已答内容与剩余时间。 */
    ExamSessionVO get(Long userId, Long sessionId);

    /** 保存作答（空 items 即心跳）。当前无 IN_PROGRESS 场次返回 1005。 */
    ExamAnswerResultVO saveAnswers(Long userId, Long sessionId, ExamAnswerRequest request);

    /** 启动第 N 场次计时（PENDING → IN_PROGRESS）。 */
    ExamSittingStartVO startSitting(Long userId, Long sessionId, int sittingNo);

    /** 离开当前分节、进入同场次下一节；最后一节等价于结束本场次。 */
    ExamSectionAdvanceVO advanceSection(Long userId, Long sessionId, int sectionNo);

    /** 提前结束整个场次。 */
    ExamSittingFinishVO finishSitting(Long userId, Long sessionId, int sittingNo);

    /** 交卷判分（幂等）。 */
    ExamResultVO submit(Long userId, Long sessionId);

    /** 成绩详情；未交卷返回 1005。 */
    ExamResultVO result(Long userId, Long sessionId);

    /** 放弃会话（不计入模考统计）。 */
    void abandon(Long userId, Long sessionId);
}
