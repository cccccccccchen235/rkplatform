package com.ruankao.service.exam.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.entity.ExamSession;
import com.ruankao.domain.entity.ExamSessionItem;
import com.ruankao.domain.entity.ExamSessionSection;
import com.ruankao.domain.entity.ExamSessionSitting;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.entity.UserAnswerRecord;
import com.ruankao.domain.enums.ExamStatus;
import com.ruankao.domain.enums.PracticeSource;
import com.ruankao.domain.enums.SittingStatus;
import com.ruankao.domain.vo.ExamResultSectionVO;
import com.ruankao.domain.vo.ExamResultSittingVO;
import com.ruankao.domain.vo.ExamResultVO;
import com.ruankao.repository.dao.RepeatWindowDao;
import com.ruankao.repository.mapper.ExamSessionItemMapper;
import com.ruankao.repository.mapper.ExamSessionMapper;
import com.ruankao.repository.mapper.ExamSessionSectionMapper;
import com.ruankao.repository.mapper.ExamSessionSittingMapper;
import com.ruankao.repository.mapper.QuestionMapper;
import com.ruankao.repository.mapper.UserAnswerRecordMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.exam.ExamConverter;
import com.ruankao.service.exam.ExamScoringService;
import com.ruankao.service.practice.JudgeResult;
import com.ruankao.service.practice.JudgeService;
import com.ruankao.service.practice.WrongBookService;
import com.ruankao.service.question.SubjectService;
import com.ruankao.service.stat.StatisticsService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 交卷判分与成绩组装（05 §5.6 第 23/24 条、06 §6.4）。
 *
 * <p>独立成类是为了让**超时自动交卷**与**用户点交卷**共用同一条判分路径：把判分写进
 * {@code ExamServiceImpl} 会让"超时"分支只能通过它自己再调一次，最终得到两份略有出入的判分。
 *
 * <p>交卷是一次性把"会话 + 逐题判分 + 流水 + 错题本 + 防重复 + 统计"写进同一个事务。
 */
@Service
public class ExamScoringServiceImpl implements ExamScoringService {

    private static final Logger log = LoggerFactory.getLogger(ExamScoringServiceImpl.class);

    /** 06 §6.7 / 10 §10.7：单题耗时超过 10 分钟按 10 分钟入库。 */
    private static final int MAX_DURATION_MS = 10 * 60 * 1000;

    private final ExamSessionMapper sessionMapper;
    private final ExamSessionSittingMapper sittingMapper;
    private final ExamSessionSectionMapper sectionMapper;
    private final ExamSessionItemMapper itemMapper;
    private final QuestionMapper questionMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final JudgeService judgeService;
    private final WrongBookService wrongBookService;
    private final RepeatWindowDao repeatWindowDao;
    private final StatisticsService statisticsService;
    private final SysConfigService sysConfigService;
    private final SubjectService subjectService;

    public ExamScoringServiceImpl(ExamSessionMapper sessionMapper,
                                  ExamSessionSittingMapper sittingMapper,
                                  ExamSessionSectionMapper sectionMapper,
                                  ExamSessionItemMapper itemMapper,
                                  QuestionMapper questionMapper,
                                  UserAnswerRecordMapper userAnswerRecordMapper,
                                  JudgeService judgeService,
                                  WrongBookService wrongBookService,
                                  RepeatWindowDao repeatWindowDao,
                                  StatisticsService statisticsService,
                                  SysConfigService sysConfigService,
                                  SubjectService subjectService) {
        this.sessionMapper = sessionMapper;
        this.sittingMapper = sittingMapper;
        this.sectionMapper = sectionMapper;
        this.itemMapper = itemMapper;
        this.questionMapper = questionMapper;
        this.userAnswerRecordMapper = userAnswerRecordMapper;
        this.judgeService = judgeService;
        this.wrongBookService = wrongBookService;
        this.repeatWindowDao = repeatWindowDao;
        this.statisticsService = statisticsService;
        this.sysConfigService = sysConfigService;
        this.subjectService = subjectService;
    }

    @Override
    @Transactional
    public ExamResultVO score(Long userId, Long sessionId) {
        ExamSession session = requireSession(userId, sessionId);
        if (ExamStatus.ABANDONED.name().equals(session.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "已放弃的会话不能交卷");
        }
        if (ExamStatus.SCORED.name().equals(session.getStatus())) {
            // 幂等：连点两次交卷只结算一次（10 §10.7 验收项）
            return buildResult(session);
        }

        LocalDateTime now = LocalDateTime.now();
        // 交卷时把仍然"活着"的场次一并收尾：
        //   · IN_PROGRESS：用户点交卷时正在作答的那个场次（正常路径）；
        //   · PENDING：用户放弃论文直接交卷（05 §5.6 第 23 条），不生成任何作答。
        // 遗漏 IN_PROGRESS 会让会话变成 SCORED 而场次仍停在 IN_PROGRESS——一个自相矛盾的终态。
        for (ExamSessionSitting sitting : sittingMapper.selectBySession(sessionId)) {
            if (SittingStatus.PENDING.name().equals(sitting.getStatus())
                    || SittingStatus.IN_PROGRESS.name().equals(sitting.getStatus())) {
                sittingMapper.finish(sitting.getId(),
                        sitting.getObjectiveScore() == null ? BigDecimal.ZERO : sitting.getObjectiveScore(),
                        now);
                sectionMapper.finishSitting(sessionId, sitting.getSittingNo(), now);
            }
        }

        List<ExamSessionItem> items = itemMapper.selectBySession(sessionId);
        Map<Long, Question> liveById = loadLiveQuestions(items);

        int answered = 0;
        int correct = 0;
        int durationMsSum = 0;
        List<Long> touchedQuestionIds = new ArrayList<>();
        Map<Integer, BigDecimal> sectionScores = new HashMap<>();

        for (ExamSessionItem item : items) {
            // 用题目快照的题型与正确答案判分：题干可以被编辑，但这次考试的结果必须可复现
            Question reference = new Question();
            reference.setQuestionType(item.getQuestionType());
            reference.setCorrectAnswer(item.getCorrectAnswer());
            JudgeResult judged = judgeService.judge(reference, item.getUserAnswer());

            BigDecimal itemScore = Boolean.TRUE.equals(judged.isCorrect())
                    ? (item.getScore() == null ? BigDecimal.ZERO : item.getScore())
                    : BigDecimal.ZERO;
            Question live = liveById.get(item.getQuestionId());
            itemMapper.updateJudgement(item.getId(), judged.isCorrect(), itemScore,
                    live == null ? null : live.getReferenceAnswer(),
                    live == null ? null : live.getAnalysis());
            sectionScores.merge(item.getSectionNo(), itemScore, BigDecimal::add);

            if (judged.scored() && !judged.blank()) {
                answered++;
                if (Boolean.TRUE.equals(judged.isCorrect())) {
                    correct++;
                }
                int durationMs = clampDuration(item.getDurationMs());
                durationMsSum += durationMs;

                UserAnswerRecord record = new UserAnswerRecord();
                record.setUserId(userId);
                record.setSubjectId(session.getSubjectId());
                record.setOutlineId(live == null || live.getOutlineId() == null ? 0L : live.getOutlineId());
                record.setQuestionId(item.getQuestionId());
                record.setSource(PracticeSource.EXAM.name());
                record.setSessionId(sessionId);
                record.setUserAnswer(item.getUserAnswer());
                record.setIsCorrect(judged.isCorrect());
                record.setDurationMs(durationMs);
                record.setAnsweredAt(item.getAnsweredAt() == null ? now : item.getAnsweredAt());
                userAnswerRecordMapper.insert(record);
                touchedQuestionIds.add(item.getQuestionId());

                if (Boolean.FALSE.equals(judged.isCorrect())) {
                    wrongBookService.onAnswered(userId, item.getQuestionId(), session.getSubjectId(), judged);
                }
            }
        }

        BigDecimal sessionScore = BigDecimal.ZERO;
        List<ExamSessionSection> sections = sectionMapper.selectBySession(sessionId);
        for (ExamSessionSection section : sections) {
            BigDecimal score = sectionScores.getOrDefault(section.getSectionNo(), BigDecimal.ZERO);
            sectionMapper.updateScore(section.getId(), score);
            sessionScore = sessionScore.add(score);
        }
        for (ExamSessionSitting sitting : sittingMapper.selectBySession(sessionId)) {
            BigDecimal sittingScore = BigDecimal.ZERO;
            for (ExamSessionSection section : sections) {
                if (section.getSittingNo().equals(sitting.getSittingNo())) {
                    sittingScore = sittingScore.add(sectionScores.getOrDefault(section.getSectionNo(), BigDecimal.ZERO));
                }
            }
            sittingMapper.updateProgress(sitting.getId(), 0, sitting.getCurrentSectionNo(), sittingScore);
        }

        if (!touchedQuestionIds.isEmpty()) {
            repeatWindowDao.add(userId, session.getSubjectId(), touchedQuestionIds,
                    Duration.ofDays(sysConfigService.repeatWindowDays()));
        }
        if (answered > 0) {
            statisticsService.incrementCounters(userId, session.getSubjectId(),
                    answered, correct, durationMsSum / 1000, now);
        }
        statisticsService.recordExam(userId, session.getSubjectId(), sessionScore);

        sessionMapper.markSubmitted(sessionId, now);
        sessionMapper.markScored(sessionId, sessionScore, now);
        log.info("模拟考交卷完成：sessionId={}, userId={}, 作答={}, 正确={}, 得分={}",
                sessionId, userId, answered, correct, sessionScore);

        return buildResult(requireSession(userId, sessionId));
    }

    @Override
    public ExamResultVO result(Long userId, Long sessionId) {
        ExamSession session = requireSession(userId, sessionId);
        if (!ExamStatus.SCORED.name().equals(session.getStatus())
                && !ExamStatus.SUBMITTED.name().equals(session.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "尚未交卷，无法查看成绩");
        }
        return buildResult(session);
    }

    private ExamResultVO buildResult(ExamSession session) {
        List<ExamSessionSitting> sittings = sittingMapper.selectBySession(session.getId());
        List<ExamSessionSection> sections = sectionMapper.selectBySession(session.getId());
        List<ExamSessionItem> items = itemMapper.selectBySession(session.getId());
        Map<Long, Question> liveById = loadLiveQuestions(items);

        List<ExamResultSittingVO> sittingResults = new ArrayList<>(sittings.size());
        for (ExamSessionSitting sitting : sittings) {
            sittingResults.add(ExamConverter.toResultSitting(sitting, sectionsOf(sections, sitting.getSittingNo())));
        }
        List<ExamResultSectionVO> sectionResults = new ArrayList<>(sections.size());
        for (ExamSessionSection section : sections) {
            sectionResults.add(ExamConverter.toResultSection(section));
        }
        List<com.ruankao.domain.vo.QuestionDetailVO> itemVos = new ArrayList<>(items.size());
        for (ExamSessionItem item : items) {
            itemVos.add(ExamConverter.toResultItem(item, liveById.get(item.getQuestionId())));
        }

        int objectiveTotal = 0;
        int correct = 0;
        int usedSec = 0;
        for (ExamSessionItem item : items) {
            if (item.getIsCorrect() != null) {
                objectiveTotal++;
                if (Boolean.TRUE.equals(item.getIsCorrect())) {
                    correct++;
                }
            }
        }
        for (ExamSessionSection section : sections) {
            usedSec += section.getUsedSec() == null ? 0 : section.getUsedSec();
        }
        BigDecimal accuracy = objectiveTotal == 0
                ? BigDecimal.ZERO.setScale(4)
                : BigDecimal.valueOf(correct).divide(BigDecimal.valueOf(objectiveTotal), 4, java.math.RoundingMode.HALF_UP);

        return new ExamResultVO(
                String.valueOf(session.getId()),
                session.getSessionNo(),
                subjectService.getDetail(session.getSubjectId()).name(),
                session.getObjectiveScore(),
                session.getAutoScoredMaxScore(),
                session.getTotalMaxScore(),
                accuracy,
                usedSec,
                session.getSubmittedAt(),
                session.getScoredAt(),
                sittingResults,
                sectionResults,
                itemVos);
    }

    private static List<ExamSessionSection> sectionsOf(List<ExamSessionSection> sections, Integer sittingNo) {
        List<ExamSessionSection> result = new ArrayList<>();
        for (ExamSessionSection section : sections) {
            if (section.getSittingNo().equals(sittingNo)) {
                result.add(section);
            }
        }
        return result;
    }

    private Map<Long, Question> loadLiveQuestions(List<ExamSessionItem> items) {
        Set<Long> ids = new HashSet<>();
        for (ExamSessionItem item : items) {
            ids.add(item.getQuestionId());
        }
        Map<Long, Question> byId = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (Question question : questionMapper.selectByIds(new ArrayList<>(ids))) {
                byId.put(question.getId(), question);
            }
        }
        return byId;
    }

    private ExamSession requireSession(Long userId, Long sessionId) {
        ExamSession session = sessionMapper.selectByIdAndUser(sessionId, userId);
        if (session == null) {
            throw new BizException(ErrorCode.SESSION_NOT_FOUND);
        }
        return session;
    }

    private static int clampDuration(Integer raw) {
        if (raw == null || raw < 0) {
            return 0;
        }
        return Math.min(raw, MAX_DURATION_MS);
    }
}
