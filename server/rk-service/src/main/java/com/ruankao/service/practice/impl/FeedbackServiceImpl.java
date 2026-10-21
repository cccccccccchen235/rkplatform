package com.ruankao.service.practice.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.FeedbackRequest;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.entity.QuestionFeedback;
import com.ruankao.repository.dao.FeedbackLimitDao;
import com.ruankao.repository.mapper.QuestionFeedbackMapper;
import com.ruankao.repository.mapper.QuestionMapper;
import com.ruankao.service.practice.FeedbackService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 题目反馈实现（05 §5.9）。
 *
 * <p>顺序很重要：**先确认题目存在、再计数、最后落库**。
 * 若先计数再校验题目，"往一个不存在的题目狂发反馈"也会消耗掉限流额度并留下脏计数；
 * 而先计数再落库（而不是落库后计数）保证了"超限的那一次不产生反馈行"。
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final QuestionMapper questionMapper;
    private final QuestionFeedbackMapper questionFeedbackMapper;
    private final FeedbackLimitDao feedbackLimitDao;

    public FeedbackServiceImpl(QuestionMapper questionMapper,
                               QuestionFeedbackMapper questionFeedbackMapper,
                               FeedbackLimitDao feedbackLimitDao) {
        this.questionMapper = questionMapper;
        this.questionFeedbackMapper = questionFeedbackMapper;
        this.feedbackLimitDao = feedbackLimitDao;
    }

    @Override
    public void submit(Long userId, Long questionId, FeedbackRequest request) {
        List<Question> found = questionMapper.selectByIds(List.of(questionId));
        if (found.isEmpty()) {
            throw new BizException(ErrorCode.QUESTION_NOT_FOUND);
        }

        long attempts = feedbackLimitDao.recordSubmit(userId, questionId);
        if (attempts > FeedbackLimitDao.MAX_PER_WINDOW) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
        }

        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setQuestionId(questionId);
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setContent(request.getContent());
        // 初始状态由 DDL 默认值给出（PENDING），这里显式写一遍以便阅读，
        // 也不依赖"将来有人改了 DDL 默认值"这种隐性契约。
        feedback.setStatus("PENDING");
        feedback.setCreatedAt(LocalDateTime.now());
        questionFeedbackMapper.insert(feedback);
    }
}