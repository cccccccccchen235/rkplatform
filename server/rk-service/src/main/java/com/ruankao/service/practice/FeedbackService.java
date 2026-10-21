package com.ruankao.service.practice;

import com.ruankao.domain.dto.FeedbackRequest;

/**
 * 题目反馈（05 §5.9 第 36 条）。
 *
 * <p>限流用 Redis 固定窗口计数器（{@code rk:limit:feedback:{userId}:{questionId}}），
 * 同题同用户 24 小时内最多 3 次，第 4 次返回 {@code 1002}
 * （验收项「题目反馈提交成功，同题同用户第 4 次提交返回 1002」）。
 */
public interface FeedbackService {

    /**
     * 提交反馈。
     *
     * @throws com.ruankao.common.exception.BizException 3002：题目不存在
     * @throws com.ruankao.common.exception.BizException 1002：24 小时内同题提交超过 3 次
     */
    void submit(Long userId, Long questionId, FeedbackRequest request);
}