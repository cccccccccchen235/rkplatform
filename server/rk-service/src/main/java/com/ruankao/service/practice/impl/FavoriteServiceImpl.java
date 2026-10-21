package com.ruankao.service.practice.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.api.PageQuery;
import com.ruankao.common.api.PageResult;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.vo.QuestionVO;
import com.ruankao.repository.mapper.QuestionMapper;
import com.ruankao.repository.mapper.UserFavoriteMapper;
import com.ruankao.service.practice.FavoriteService;
import com.ruankao.service.practice.QuestionAssemblyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 收藏实现（05 §5.7）。
 *
 * <p>{@code subject_id} 由服务端从 {@code rk_question} 反查，**不收客户端传的科目**——
 * 收藏行冗余了这一列（04 §4.3 的注释：避免列表查询回表），一旦客户端传错，
 * 这条收藏就会从"该科目的收藏列表"里消失，而错因不在查询侧。
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final QuestionMapper questionMapper;
    private final QuestionAssemblyService questionAssemblyService;

    public FavoriteServiceImpl(UserFavoriteMapper userFavoriteMapper,
                               QuestionMapper questionMapper,
                               QuestionAssemblyService questionAssemblyService) {
        this.userFavoriteMapper = userFavoriteMapper;
        this.questionMapper = questionMapper;
        this.questionAssemblyService = questionAssemblyService;
    }

    @Override
    public void add(Long userId, Long questionId) {
        Question question = loadQuestion(questionId);
        // INSERT IGNORE：重复收藏靠 uk_user_question 吞掉，不先查一次"是否已收藏"
        // （那是读改写，且有竞态）。
        userFavoriteMapper.insertIgnore(userId, questionId, question.getSubjectId());
    }

    @Override
    public void remove(Long userId, Long questionId) {
        // 幂等：删不到也当成功，见接口注释
        userFavoriteMapper.delete(userId, questionId);
    }

    @Override
    public PageResult<QuestionVO> list(Long userId, Long subjectId, PageQuery page) {
        long total = userFavoriteMapper.countBySubject(userId, subjectId);
        if (total == 0) {
            return PageResult.empty(page);
        }
        List<Long> ids = userFavoriteMapper.selectQuestionIds(userId, subjectId, page.offset(), page.limit());
        if (ids.isEmpty()) {
            return PageResult.empty(page);
        }

        List<Question> questions = questionMapper.selectByIds(ids);
        // selectByIds 按主键返回，而收藏列表的契约是"按收藏时间倒序"（05 §5.7）。
        // 顺序由 ids 决定，所以在这里按 ids 重排，而不是让 SQL 再排一次。
        Map<Long, Question> byId = new LinkedHashMap<>();
        for (Question question : questions) {
            byId.put(question.getId(), question);
        }
        List<Question> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();

        return PageResult.of(questionAssemblyService.toVOList(userId, ordered), total, page);
    }

    /** 取题目；不存在抛 3002（05 §5.1.2）。 */
    private Question loadQuestion(Long questionId) {
        List<Question> found = questionMapper.selectByIds(List.of(questionId));
        if (found.isEmpty()) {
            throw new BizException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return found.get(0);
    }
}