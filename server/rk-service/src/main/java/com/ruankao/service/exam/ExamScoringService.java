package com.ruankao.service.exam;

import com.ruankao.domain.vo.ExamResultVO;

/**
 * 交卷判分与成绩组装（05 §5.6 第 23/24 条）。
 *
 * <p>拆出接口的目的是让"用户点交卷"与"超时自动交卷"走同一条路径——
 * 两份判分实现早晚会在某个边界（空作答、主观题、超额得分）上产生分歧，
 * 而模拟考成绩不一致比功能缺失更难排查。
 */
public interface ExamScoringService {

    /** 判分并结算（幂等：已 SCORED 直接返回既有成绩）。 */
    ExamResultVO score(Long userId, Long sessionId);

    /** 只读成绩；未交卷返回 1005。 */
    ExamResultVO result(Long userId, Long sessionId);
}
