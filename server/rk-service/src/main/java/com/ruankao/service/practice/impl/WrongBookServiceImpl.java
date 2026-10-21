package com.ruankao.service.practice.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.api.PageQuery;
import com.ruankao.common.api.PageResult;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.enums.WrongQuestionStatus;
import com.ruankao.domain.vo.WrongQuestionRowVO;
import com.ruankao.domain.vo.WrongQuestionVO;
import com.ruankao.repository.mapper.UserWrongQuestionMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.practice.JudgeResult;
import com.ruankao.service.practice.QuestionAssemblyService;
import com.ruankao.service.practice.WrongBookService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 错题本实现（06 §6.8）。
 *
 * <p>它是"每答一题调一次"的热路径，所以写路径只有一条 upsert SQL
 * （{@code INSERT ... ON DUPLICATE KEY UPDATE}），不做"先读再判断再写"——
 * 后者在并发下会丢更新，而依赖 {@code uk_user_question(user_id, question_id)} 的
 * 原子 upsert 把竞态交给了数据库。
 */
@Service
public class WrongBookServiceImpl implements WrongBookService {

    /** 05 §5.7 第 26 条：默认只看 ACTIVE。 */
    private static final String DEFAULT_STATUS = WrongQuestionStatus.ACTIVE.name();

    private final UserWrongQuestionMapper wrongQuestionMapper;
    private final SysConfigService sysConfigService;
    private final QuestionAssemblyService questionAssemblyService;

    public WrongBookServiceImpl(UserWrongQuestionMapper wrongQuestionMapper,
                                SysConfigService sysConfigService,
                                QuestionAssemblyService questionAssemblyService) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.sysConfigService = sysConfigService;
        this.questionAssemblyService = questionAssemblyService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>三条早退，都是刻意的：
     * <ol>
     *   <li>{@code blank}（未作答）——{@link JudgeResult#blank()} 已经把它和"答错"分开，
     *       这里直接不写。若把未作答也当成答错，用户"跳到下一题"这个动作就会往错题本里
     *       灌题，而错题本是用来复盘"不会做"的（06 §6.8）。</li>
     *   <li>{@code isCorrect == null}（主观题/论文）——不判分的题没有"对错"可记，
     *       它的答案是参考答案，进错题本没有意义。</li>
     *   <li>无 userId（游客）——样例题不产生错题记录。</li>
     * </ol>
     */
    @Override
    public void onAnswered(Long userId, Long questionId, Long subjectId, JudgeResult result) {
        if (userId == null || result == null || result.blank() || !result.scored()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(result.isCorrect())) {
            // UPDATE-only：一道从没答错过的题，答对不该在错题本里新造一行。
            wrongQuestionMapper.upsertOnCorrect(userId, questionId, subjectId, now,
                    sysConfigService.wrongbookMasterThreshold());
        } else {
            wrongQuestionMapper.upsertOnWrong(userId, questionId, subjectId, now);
        }
    }

    @Override
    public PageResult<WrongQuestionVO> list(Long userId, Long subjectId, String status,
                                            Long outlineId, Long kpId, String sortBy, PageQuery page) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedSort = normalizeSortBy(sortBy);

        long total = wrongQuestionMapper.countPage(userId, subjectId, normalizedStatus,
                outlineId, kpId);
        if (total == 0) {
            return PageResult.empty(page);
        }
        List<WrongQuestionRowVO> rows = wrongQuestionMapper.selectPage(userId, subjectId, normalizedStatus,
                outlineId, kpId, normalizedSort, page.offset(), page.limit());

        // 章节路径来自大纲树（1 小时缓存），一次装配全部行，不逐行查
        Map<Long, List<String>> paths = questionAssemblyService.outlinePaths(subjectId);

        List<WrongQuestionVO> list = rows.stream()
                .map(row -> new WrongQuestionVO(
                        String.valueOf(row.getQuestionId()),
                        row.getStemSummary(),
                        paths.getOrDefault(row.getOutlineId(), List.of()),
                        row.getWrongCount(),
                        row.getCorrectCount(),
                        row.getConsecutiveCorrect(),
                        row.getLastAnsweredAt(),
                        row.getStatus()))
                .toList();
        return PageResult.of(list, total, page);
    }

    @Override
    public void remove(Long userId, Long questionId) {
        if (wrongQuestionMapper.markRemoved(userId, questionId) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "该题不在你的错题本中");
        }
    }

    @Override
    public void restore(Long userId, Long questionId) {
        // 恢复时把连续答对清零：否则一道"已答对 1 次后被移除"的题恢复回来只差一次就 MASTERED，
        // 而用户此刻的认知是"这题我错了，重新开始"（06 §6.8 的最后一行）。
        if (wrongQuestionMapper.restore(userId, questionId) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "该题不在你的错题本中");
        }
    }

    /**
     * 归一化 status 过滤值：缺省/空 → {@code ACTIVE}；非法取值报 1001。
     *
     * <p>与科目列表的 {@code level} 过滤不同（那种非法值静默成空列表），这里**报 1001**：
     * {@code status} 决定用户看到的是哪一批题，静默回落会让人以为"我的错题都消失了"，
     * 而实际上是前端传错了一个枚举值。
     */
    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return DEFAULT_STATUS;
        }
        String upper = status.trim().toUpperCase();
        try {
            return WrongQuestionStatus.valueOf(upper).name();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "错题状态取值非法");
        }
    }

    /** 排序白名单（05 §5.7 第 26 条），非法取值回落默认，不打断列表。 */
    private static String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "lastAnsweredAt";
        }
        String trimmed = sortBy.trim();
        return "wrongCount".equals(trimmed) ? "wrongCount" : "lastAnsweredAt";
    }
}