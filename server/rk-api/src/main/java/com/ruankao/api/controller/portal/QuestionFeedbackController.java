package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.FeedbackRequest;
import com.ruankao.service.practice.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目反馈（05 §5.9 第 36 条）。
 *
 * <p>路径是 {@code /questions/{id}/feedback} 而不是 {@code /feedback}：
 * 反馈永远依附于一道具体的题，题 ID 属于路径（资源的父），不属于请求体。
 */
@RestController
@RequestMapping("/api/v1/questions")
@Validated
@Tag(name = "题目反馈", description = "05 §5.9：提交题目反馈与版权投诉")
public class QuestionFeedbackController {

    private final FeedbackService feedbackService;

    public QuestionFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** 提交反馈（05 §5.9 第 36 条）。同题同用户 24 小时内最多 3 次，第 4 次返回 1002。 */
    @PostMapping("/{id}/feedback")
    @SaCheckLogin
    @Operation(summary = "提交题目反馈",
            description = "type 取值见 02 §2.6.3；同题同用户 24 小时内最多 3 次，超出返回 1002。")
    public R<Void> submit(@PathVariable("id") Long questionId,
                          @Valid @RequestBody FeedbackRequest request) {
        feedbackService.submit(StpUtil.getLoginIdAsLong(), questionId, request);
        return R.ok();
    }
}