package com.ruankao.service.exam.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.api.PageResult;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.ExamAnswerRequest;
import com.ruankao.domain.entity.ExamSession;
import com.ruankao.domain.entity.ExamSessionItem;
import com.ruankao.domain.entity.ExamSessionSection;
import com.ruankao.domain.entity.ExamSessionSitting;
import com.ruankao.domain.entity.PaperSection;
import com.ruankao.domain.entity.PaperSitting;
import com.ruankao.domain.entity.PaperTemplate;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.entity.QuestionOption;
import com.ruankao.domain.enums.ExamStatus;
import com.ruankao.domain.enums.QuestionType;
import com.ruankao.domain.enums.SectionStatus;
import com.ruankao.domain.enums.SittingStatus;
import com.ruankao.domain.vo.ExamAnswerResultVO;
import com.ruankao.domain.vo.ExamHistoryVO;
import com.ruankao.domain.vo.ExamItemVO;
import com.ruankao.domain.vo.ExamQuestionCandidate;
import com.ruankao.domain.vo.ExamResultVO;
import com.ruankao.domain.vo.ExamSectionAdvanceVO;
import com.ruankao.domain.vo.ExamSectionVO;
import com.ruankao.domain.vo.ExamSessionVO;
import com.ruankao.domain.vo.ExamSittingFinishVO;
import com.ruankao.domain.vo.ExamSittingStartVO;
import com.ruankao.domain.vo.ExamSittingVO;
import com.ruankao.repository.dao.RepeatWindowDao;
import com.ruankao.repository.mapper.ExamQuestionMapper;
import com.ruankao.repository.mapper.ExamSessionItemMapper;
import com.ruankao.repository.mapper.ExamSessionMapper;
import com.ruankao.repository.mapper.ExamSessionSectionMapper;
import com.ruankao.repository.mapper.ExamSessionSittingMapper;
import com.ruankao.repository.mapper.PaperTemplateMapper;
import com.ruankao.repository.mapper.QuestionMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.exam.ExamConverter;
import com.ruankao.service.exam.ExamPaperAssembler;
import com.ruankao.service.exam.ExamScoringService;
import com.ruankao.service.exam.ExamService;
import com.ruankao.service.exam.ExamTiming;
import com.ruankao.service.question.SubjectService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 模拟机考闭环实现（05 §5.6）。口径说明见 {@link ExamService}。 */
@Service
public class ExamServiceImpl implements ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamServiceImpl.class);

    private static final int CANDIDATE_LIMIT = 5000;
    private static final int ANSWER_ITEM_LIMIT = 100;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_DURATION_MS = 10 * 60 * 1000;
    private static final DateTimeFormatter SESSION_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExamSessionMapper sessionMapper;
    private final ExamSessionSittingMapper sittingMapper;
    private final ExamSessionSectionMapper sectionMapper;
    private final ExamSessionItemMapper itemMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final PaperTemplateMapper paperTemplateMapper;
    private final ExamTimeoutSettler timeoutSettler;
    private final ExamScoringService scoringService;
    private final RepeatWindowDao repeatWindowDao;
    private final SysConfigService sysConfigService;
    private final SubjectService subjectService;

    public ExamServiceImpl(ExamSessionMapper sessionMapper,
                           ExamSessionSittingMapper sittingMapper,
                           ExamSessionSectionMapper sectionMapper,
                           ExamSessionItemMapper itemMapper,
                           ExamQuestionMapper examQuestionMapper,
                           QuestionMapper questionMapper,
                           PaperTemplateMapper paperTemplateMapper,
                           ExamTimeoutSettler timeoutSettler,
                           ExamScoringService scoringService,
                           RepeatWindowDao repeatWindowDao,
                           SysConfigService sysConfigService,
                           SubjectService subjectService) {
        this.sessionMapper = sessionMapper;
        this.sittingMapper = sittingMapper;
        this.sectionMapper = sectionMapper;
        this.itemMapper = itemMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.questionMapper = questionMapper;
        this.paperTemplateMapper = paperTemplateMapper;
        this.timeoutSettler = timeoutSettler;
        this.scoringService = scoringService;
        this.repeatWindowDao = repeatWindowDao;
        this.sysConfigService = sysConfigService;
        this.subjectService = subjectService;
    }

    // ---------------------------------------------------------------- 发起

    @Override
    @Transactional
    public ExamSessionVO create(Long userId, Long subjectId) {
        String subjectName = subjectService.getDetail(subjectId).name();
        PaperTemplate template = paperTemplateMapper.selectEnabledBySubjectId(subjectId);
        if (template == null) {
            throw new BizException(ErrorCode.PAPER_TEMPLATE_MISSING);
        }
        if (sessionMapper.selectActiveByUser(userId) != null) {
            throw new BizException(ErrorCode.SESSION_UNFINISHED_EXISTS);
        }

        List<PaperSitting> templateSittings = paperTemplateMapper.selectSittings(template.getId());
        List<PaperSection> templateSections = paperTemplateMapper.selectSections(template.getId());
        boolean hasScorable = templateSections.stream()
                .anyMatch(section -> isSupported(section) && isObjectiveType(section.getQuestionType()));
        if (templateSittings.isEmpty() || !hasScorable) {
            throw new BizException(ErrorCode.PAPER_NO_ANSWERABLE_SECTION);
        }

        Set<Long> repeated = repeatWindowDao.members(userId, subjectId, sysConfigService.repeatWindowDays());
        Random random = new Random();

        List<ExamSessionSection> sectionRows = new ArrayList<>();
        List<ExamSessionItem> itemRows = new ArrayList<>();
        int sequence = 0;
        int autoScoredMax = 0;
        int totalMax = 0;
        int repeatedCount = 0;

        for (PaperSection section : templateSections) {
            int firstIndex = itemRows.size();
            if (isSupported(section) && section.getQuestionCount() != null && section.getQuestionCount() > 0) {
                List<ExamQuestionCandidate> candidates = examQuestionMapper.selectCandidates(
                        userId, subjectId, section.getQuestionType(), CANDIDATE_LIMIT);
                ExamPaperAssembler.Selection selection = ExamPaperAssembler.select(
                        candidates, section.getQuestionCount(), repeated, random);
                repeatedCount += selection.repeatedCount();
                sequence = appendItems(userId, itemRows, section, selection.questionIds(), sequence);
            }
            int actual = itemRows.size() - firstIndex;
            int maxScore = section.getPerQuestionScore() == null ? 0
                    : section.getPerQuestionScore().multiply(BigDecimal.valueOf(actual)).intValue();
            if (isSupported(section)) {
                totalMax += maxScore;
                if ("OBJECTIVE".equals(section.getSectionType())) {
                    autoScoredMax += maxScore;
                }
            }
            sectionRows.add(buildSection(section, actual, maxScore));
        }

        if (itemRows.isEmpty()) {
            throw new BizException(ErrorCode.PAPER_NO_ANSWERABLE_SECTION);
        }

        LocalDateTime now = LocalDateTime.now();
        List<ExamSessionSitting> sittingRows = new ArrayList<>();
        int totalDurationSec = 0;
        for (PaperSitting sitting : templateSittings) {
            ExamSessionSitting row = new ExamSessionSitting();
            row.setSittingNo(sitting.getSittingNo());
            row.setSittingName(sitting.getSittingName());
            int duration = (sitting.getTotalDuration() == null ? 0 : sitting.getTotalDuration()) * 60;
            row.setTotalDurationSec(duration);
            row.setRemainingSec(duration);
            row.setStatus(SittingStatus.PENDING.name());
            row.setCurrentSectionNo(firstSectionNo(sectionRows, sitting.getSittingNo()));
            row.setObjectiveScore(BigDecimal.ZERO);
            totalDurationSec += duration;
            sittingRows.add(row);
        }

        ExamSession session = new ExamSession();
        session.setSessionNo(nextSessionNo(now));
        session.setUserId(userId);
        session.setSubjectId(subjectId);
        session.setTemplateId(template.getId());
        session.setStatus(ExamStatus.CREATED.name());
        session.setTotalDurationSec(totalDurationSec);
        session.setRemainingSec(0);
        session.setCurrentSittingNo(sittingRows.get(0).getSittingNo());
        session.setCurrentSectionNo(firstSectionNo(sectionRows, sittingRows.get(0).getSittingNo()));
        session.setQuestionCount(itemRows.size());
        session.setObjectiveScore(BigDecimal.ZERO);
        session.setAutoScoredMaxScore(autoScoredMax);
        session.setTotalMaxScore(totalMax);
        sessionMapper.insert(session);

        sectionRows.forEach(row -> row.setSessionId(session.getId()));
        itemRows.forEach(row -> row.setSessionId(session.getId()));
        sittingRows.forEach(row -> row.setSessionId(session.getId()));
        sectionMapper.insertBatch(sectionRows);
        itemMapper.insertBatch(itemRows);
        sittingMapper.insertBatch(sittingRows);

        log.info("发起模拟考：sessionId={}, userId={}, subjectId={}, 题量={}, 复用旧题={}",
                session.getId(), userId, subjectId, itemRows.size(), repeatedCount);
        return toSessionVO(session, subjectName);
    }

    /** 为某一分节抽取题目并生成快照；返回新的全卷题序。 */
    private int appendItems(Long userId, List<ExamSessionItem> itemRows, PaperSection section,
                            List<Long> questionIds, int sequence) {
        Map<Long, Question> byId = new LinkedHashMap<>();
        for (Question question : questionMapper.selectByIds(questionIds)) {
            byId.put(question.getId(), question);
        }
        Map<Long, List<QuestionOption>> optionsById = new LinkedHashMap<>();
        if (!questionIds.isEmpty()) {
            for (QuestionOption option : questionMapper.selectOptionsByQuestionIds(questionIds)) {
                optionsById.computeIfAbsent(option.getQuestionId(), k -> new ArrayList<>()).add(option);
            }
        }
        int current = sequence;
        for (Long questionId : questionIds) {
            Question question = byId.get(questionId);
            if (question == null) {
                continue;
            }
            current++;
            ExamSessionItem item = new ExamSessionItem();
            item.setSectionNo(section.getSectionNo());
            item.setSequence(current);
            item.setQuestionId(questionId);
            item.setQuestionType(question.getQuestionType());
            item.setStemSnapshot(question.getStem());
            item.setOptionsSnapshot(serializeOptions(optionsById.get(questionId)));
            item.setCorrectAnswer(question.getCorrectAnswer());
            item.setScore(section.getPerQuestionScore());
            itemRows.add(item);
        }
        return current;
    }

    private static ExamSessionSection buildSection(PaperSection template, int actual, int maxScore) {
        ExamSessionSection section = new ExamSessionSection();
        section.setSittingNo(template.getSittingNo());
        section.setSectionNo(template.getSectionNo());
        section.setSectionName(template.getSectionName());
        section.setSectionType(template.getSectionType());
        section.setQuestionCount(actual);
        section.setMaxScore(BigDecimal.valueOf(maxScore));
        section.setObjectiveScore(BigDecimal.ZERO);
        section.setDurationMin(template.getDurationMin());
        section.setDurationMax(template.getDurationMax());
        section.setAllowEarlySubmit(template.getAllowEarlySubmit());
        section.setSupportStatus(template.getSupportStatus());
        section.setUsedSec(0);
        section.setStatus(isSupported(template)
                ? SectionStatus.PENDING.name()
                : SectionStatus.SKIPPED.name());
        return section;
    }

    // ---------------------------------------------------------------- 读取

    @Override
    public ExamSessionVO get(Long userId, Long sessionId) {
        timeoutSettler.settleExpired(userId, sessionId);
        ExamSession session = requireSession(userId, sessionId);
        return toSessionVO(session, subjectService.getDetail(session.getSubjectId()).name());
    }

    @Override
    public PageResult<ExamHistoryVO> history(Long userId, Long subjectId, boolean includeAbandoned,
                                             int pageNum, int pageSize) {
        int page = Math.max(1, pageNum);
        int size = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        long total = sessionMapper.countHistory(userId, subjectId, includeAbandoned);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        List<ExamSession> rows = sessionMapper.selectHistory(userId, subjectId, includeAbandoned,
                size, (page - 1) * size);

        Map<Long, String> subjectNames = new LinkedHashMap<>();
        List<ExamHistoryVO> list = new ArrayList<>(rows.size());
        for (ExamSession row : rows) {
            String name = subjectNames.computeIfAbsent(row.getSubjectId(),
                    id -> safeSubjectName(id));
            list.add(new ExamHistoryVO(
                    String.valueOf(row.getId()),
                    row.getSessionNo(),
                    String.valueOf(row.getSubjectId()),
                    name,
                    row.getStatus(),
                    row.getObjectiveScore(),
                    row.getAutoScoredMaxScore(),
                    accuracyOf(row),
                    usedSecOf(row.getId()),
                    row.getSubmittedAt()));
        }
        return new PageResult<>(list, total, page, size);
    }

    @Override
    public ExamResultVO result(Long userId, Long sessionId) {
        return scoringService.result(userId, sessionId);
    }

    // ---------------------------------------------------------------- 作答

    @Override
    @Transactional
    public ExamAnswerResultVO saveAnswers(Long userId, Long sessionId, ExamAnswerRequest request) {
        if (timeoutSettler.settleExpired(userId, sessionId)) {
            throw new BizException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        ExamSession session = requireSession(userId, sessionId);
        if (!ExamStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            // CREATED（还没开始任何场次）与"已结束"是两件事：前者按 05 §5.6 第 19 条返回 1005
            // （"请先开始本场次"），后者才是 4005。混成 4005 会让前端以为考试已经结束。
            if (ExamStatus.CREATED.name().equals(session.getStatus())) {
                throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "请先开始本场次");
            }
            throw new BizException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        List<ExamSessionSitting> sittings = sittingMapper.selectBySession(sessionId);
        ExamSessionSitting current = findInProgress(sittings);
        if (current == null) {
            // PENDING 场次不接受作答：必须先调 20 启动计时（05 §5.6 第 19 条）
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "请先开始本场次");
        }

        LocalDateTime now = LocalDateTime.now();
        List<ExamAnswerRequest.Item> items = request.getItems();
        if (items != null && !items.isEmpty()) {
            if (items.size() > ANSWER_ITEM_LIMIT) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单次最多保存 100 条作答");
            }
            List<Long> itemIds = new ArrayList<>(items.size());
            Set<Long> distinct = new HashSet<>();
            for (ExamAnswerRequest.Item item : items) {
                Long itemId = parseId(item.getItemId());
                if (itemId == null) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "itemId 必须是数字");
                }
                itemIds.add(itemId);
                distinct.add(itemId);
            }
            if (itemMapper.selectByIds(sessionId, itemIds).size() != distinct.size()) {
                throw new BizException(ErrorCode.QUESTION_NOT_IN_SESSION);
            }
            for (ExamAnswerRequest.Item item : items) {
                itemMapper.updateAnswer(parseId(item.getItemId()), sessionId,
                        normalize(item.getUserAnswer()), clampDuration(item.getDurationMs()), now);
            }
        }

        ExamSessionSection currentSection = sectionMapper.selectOne(sessionId, current.getCurrentSectionNo());
        if (currentSection != null) {
            LocalDateTime sectionStart = currentSection.getStartedAt() == null
                    ? current.getStartedAt() : currentSection.getStartedAt();
            sectionMapper.updateUsedSec(currentSection.getId(), ExamTiming.usedSec(sectionStart, now));
        }

        int remaining = ExamTiming.remainingSec(current.getStartedAt(), current.getTotalDurationSec(), now);
        sittingMapper.updateProgress(current.getId(), remaining, current.getCurrentSectionNo(),
                current.getObjectiveScore());
        sessionMapper.updateProgress(sessionId, ExamStatus.IN_PROGRESS.name(), current.getSittingNo(),
                current.getCurrentSectionNo(), remaining, now);
        return new ExamAnswerResultVO(remaining, current.getSittingNo(), current.getCurrentSectionNo());
    }

    // ---------------------------------------------------------------- 场次与分节

    @Override
    @Transactional
    public ExamSittingStartVO startSitting(Long userId, Long sessionId, int sittingNo) {
        if (timeoutSettler.settleExpired(userId, sessionId)) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "上一场次已超时结束");
        }
        ExamSession session = requireSession(userId, sessionId);
        if (ExamStatus.ABANDONED.name().equals(session.getStatus())
                || ExamStatus.SUBMITTED.name().equals(session.getStatus())
                || ExamStatus.SCORED.name().equals(session.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "会话已结束");
        }
        ExamSessionSitting sitting = sittingMapper.selectOne(sessionId, sittingNo);
        if (sitting == null) {
            throw new BizException(ErrorCode.SESSION_NOT_FOUND, "场次不存在");
        }
        if (!SittingStatus.PENDING.name().equals(sitting.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "该场次已开始或已结束");
        }
        if (findInProgress(sittingMapper.selectBySession(sessionId)) != null) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "已有进行中的场次");
        }

        LocalDateTime now = LocalDateTime.now();
        if (sittingMapper.start(sitting.getId(), now, sitting.getTotalDurationSec()) == 0) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "该场次已开始或已结束");
        }
        List<ExamSessionSection> sections = sectionMapper.selectBySession(sessionId);
        int currentSectionNo = firstSectionNo(sections, sittingNo);
        startSection(sections, sittingNo, currentSectionNo, now);
        sessionMapper.updateProgress(sessionId, ExamStatus.IN_PROGRESS.name(), sittingNo,
                currentSectionNo, sitting.getTotalDurationSec(), now);

        ExamSessionSitting started = sittingMapper.selectOne(sessionId, sittingNo);
        return new ExamSittingStartVO(
                ExamConverter.toSittingVO(started, sectionsOf(sections, sittingNo), now),
                ExamTiming.remainingSec(started.getStartedAt(), started.getTotalDurationSec(), now),
                currentSectionNo);
    }

    @Override
    @Transactional
    public ExamSectionAdvanceVO advanceSection(Long userId, Long sessionId, int sectionNo) {
        if (timeoutSettler.settleExpired(userId, sessionId)) {
            throw new BizException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        ExamSession session = requireSession(userId, sessionId);
        ExamSessionSitting sitting = findInProgress(sittingMapper.selectBySession(sessionId));
        if (sitting == null) {
            throw new BizException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        List<ExamSessionSection> sections = sectionMapper.selectBySession(sessionId);
        ExamSessionSection section = findSection(sections, sectionNo);
        if (section == null || !sitting.getCurrentSectionNo().equals(sectionNo)) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "只能离开当前分节");
        }
        if (!isSupportedSection(section)) {
            throw new BizException(ErrorCode.SECTION_NOT_SUPPORTED);
        }

        LocalDateTime now = LocalDateTime.now();
        int usedSec = ExamTiming.usedSec(section.getStartedAt(), now);
        int minSec = (section.getDurationMin() == null ? 0 : section.getDurationMin()) * 60;
        if (usedSec < minSec) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                    "最早可在 " + (minSec - usedSec) + " 秒后离开本节");
        }
        sectionMapper.finish(section.getId(), SectionStatus.FINISHED.name(), usedSec, now);

        ExamSessionSection next = nextPendingSection(sections, sitting.getSittingNo(), sectionNo);
        if (next != null) {
            startSection(sections, sitting.getSittingNo(), next.getSectionNo(), now);
            sessionMapper.updateProgress(sessionId, ExamStatus.IN_PROGRESS.name(), sitting.getSittingNo(),
                    next.getSectionNo(), ExamTiming.remainingSec(sitting.getStartedAt(),
                            sitting.getTotalDurationSec(), now), now);
            sittingMapper.updateProgress(sitting.getId(),
                    ExamTiming.remainingSec(sitting.getStartedAt(), sitting.getTotalDurationSec(), now),
                    next.getSectionNo(), sitting.getObjectiveScore());
        } else {
            closeSitting(session, sitting, now);
        }
        ExamSession fresh = requireSession(userId, sessionId);
        List<ExamSessionSitting> freshSittings = sittingMapper.selectBySession(sessionId);
        ExamSessionSitting freshCurrent = findInProgress(freshSittings);
        List<ExamSessionSection> freshSections = sectionMapper.selectBySession(sessionId);
        int currentSectionNo = freshCurrent == null
                ? fresh.getCurrentSectionNo() : freshCurrent.getCurrentSectionNo();
        int remaining = freshCurrent == null ? 0
                : ExamTiming.remainingSec(freshCurrent.getStartedAt(), freshCurrent.getTotalDurationSec(), now);
        List<ExamSectionVO> sectionVos = new ArrayList<>(freshSections.size());
        for (ExamSessionSection row : freshSections) {
            sectionVos.add(ExamConverter.toSectionVO(row, now));
        }
        return new ExamSectionAdvanceVO(currentSectionNo, remaining, sectionVos);
    }

    @Override
    @Transactional
    public ExamSittingFinishVO finishSitting(Long userId, Long sessionId, int sittingNo) {
        if (timeoutSettler.settleExpired(userId, sessionId)) {
            throw new BizException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        ExamSession session = requireSession(userId, sessionId);
        ExamSessionSitting sitting = sittingMapper.selectOne(sessionId, sittingNo);
        if (sitting == null) {
            throw new BizException(ErrorCode.SESSION_NOT_FOUND, "场次不存在");
        }
        if (!SittingStatus.IN_PROGRESS.name().equals(sitting.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "该场次未在进行中");
        }
        List<ExamSessionSection> sections = sectionMapper.selectBySession(sessionId);
        for (ExamSessionSection section : sectionsOf(sections, sittingNo)) {
            // 论文节不允许提前结束（02 §2.4.3、10 §10.7 验收项）
            if (answerable(section) && Boolean.FALSE.equals(section.getAllowEarlySubmit())) {
                throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "本场次包含不可提前结束的分节");
            }
        }
        ExamSessionSection current = findSection(sections, sitting.getCurrentSectionNo());
        // 只对"有题可答"的当前分节校验最早可离开时间：0 题的分节（L4 缩到 0，如当前没有主观题
        // 时的案例分析/论文）没有让用户停留的理由，否则一个空分节会把用户锁满 duration_min。
        if (current != null && answerable(current)) {
            int usedSec = ExamTiming.usedSec(current.getStartedAt(), LocalDateTime.now());
            int minSec = (current.getDurationMin() == null ? 0 : current.getDurationMin()) * 60;
            if (usedSec < minSec) {
                throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                        "最早可在 " + (minSec - usedSec) + " 秒后结束本场次");
            }
        }
        closeSitting(session, sitting, LocalDateTime.now());

        ExamSession fresh = requireSession(userId, sessionId);
        LocalDateTime now = LocalDateTime.now();
        List<ExamSessionSitting> rows = sittingMapper.selectBySession(sessionId);
        List<ExamSittingVO> sittingVos = new ArrayList<>(rows.size());
        for (ExamSessionSitting row : rows) {
            sittingVos.add(ExamConverter.toSittingVO(row, sectionsOf(sections, row.getSittingNo()), now));
        }
        ExamSessionSitting inProgress = findInProgress(rows);
        int remaining = inProgress == null ? 0
                : ExamTiming.remainingSec(inProgress.getStartedAt(), inProgress.getTotalDurationSec(), now);
        return new ExamSittingFinishVO(fresh.getCurrentSittingNo(), remaining, sittingVos);
    }

    // ---------------------------------------------------------------- 交卷与放弃

    @Override
    public ExamResultVO submit(Long userId, Long sessionId) {
        timeoutSettler.settleExpired(userId, sessionId);
        return scoringService.score(userId, sessionId);
    }

    @Override
    @Transactional
    public void abandon(Long userId, Long sessionId) {
        ExamSession session = requireSession(userId, sessionId);
        if (!ExamStatus.CREATED.name().equals(session.getStatus())
                && !ExamStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "会话已结束，无法放弃");
        }
        // 不计入 stat_user_subject.exam_count（05 §5.6 第 25 条）——故这里不碰统计
        sessionMapper.markAbandoned(sessionId);
    }

    // ---------------------------------------------------------------- 内部

    /** 结束场次本体（不含"是否允许提前结束"的校验，超时路径也用这一条）。 */
    private void closeSitting(ExamSession session, ExamSessionSitting sitting, LocalDateTime now) {
        sittingMapper.finish(sitting.getId(), sitting.getObjectiveScore(), now);
        sectionMapper.finishSitting(session.getId(), sitting.getSittingNo(), now);
        ExamSessionSitting next = findPending(sittingMapper.selectBySession(session.getId()));
        if (next != null) {
            sessionMapper.updateProgress(session.getId(), ExamStatus.IN_PROGRESS.name(),
                    next.getSittingNo(), next.getCurrentSectionNo(), 0, now);
        } else {
            sessionMapper.updateProgress(session.getId(), ExamStatus.IN_PROGRESS.name(),
                    sitting.getSittingNo(), sitting.getCurrentSectionNo(), 0, now);
        }
    }

    private void startSection(List<ExamSessionSection> sections, int sittingNo,
                              int sectionNo, LocalDateTime now) {
        ExamSessionSection section = findSection(sections, sectionNo);
        if (section != null && answerable(section)) {
            sectionMapper.start(section.getId(), now);
            section.setStartedAt(now);
            section.setStatus(SectionStatus.IN_PROGRESS.name());
        }
    }

    private ExamSessionVO toSessionVO(ExamSession session, String subjectName) {
        LocalDateTime now = LocalDateTime.now();
        List<ExamSessionSitting> sittings = sittingMapper.selectBySession(session.getId());
        List<ExamSessionSection> sections = sectionMapper.selectBySession(session.getId());
        List<ExamSessionItem> items = itemMapper.selectBySession(session.getId());

        List<ExamSittingVO> sittingVos = new ArrayList<>(sittings.size());
        for (ExamSessionSitting sitting : sittings) {
            sittingVos.add(ExamConverter.toSittingVO(sitting, sectionsOf(sections, sitting.getSittingNo()), now));
        }
        List<ExamSectionVO> sectionVos = new ArrayList<>(sections.size());
        for (ExamSessionSection section : sections) {
            sectionVos.add(ExamConverter.toSectionVO(section, now));
        }
        List<ExamItemVO> itemVos = new ArrayList<>(items.size());
        for (ExamSessionItem item : items) {
            itemVos.add(ExamConverter.toItemVO(item));
        }

        ExamSessionSitting current = findInProgress(sittings);
        int currentSittingNo = current == null ? session.getCurrentSittingNo() : current.getSittingNo();
        int currentSectionNo = current == null ? session.getCurrentSectionNo() : current.getCurrentSectionNo();
        int remaining = current == null ? 0
                : ExamTiming.remainingSec(current.getStartedAt(), current.getTotalDurationSec(), now);

        return new ExamSessionVO(
                String.valueOf(session.getId()),
                session.getSessionNo(),
                String.valueOf(session.getSubjectId()),
                subjectName,
                session.getStatus(),
                currentSittingNo,
                session.getTotalDurationSec(),
                remaining,
                currentSectionNo,
                session.getQuestionCount(),
                session.getAutoScoredMaxScore(),
                session.getTotalMaxScore(),
                sittingVos,
                sectionVos,
                itemVos);
    }

    private ExamSession requireSession(Long userId, Long sessionId) {
        ExamSession session = sessionMapper.selectByIdAndUser(sessionId, userId);
        if (session == null) {
            throw new BizException(ErrorCode.SESSION_NOT_FOUND);
        }
        return session;
    }

    private String safeSubjectName(Long subjectId) {
        try {
            return subjectService.getDetail(subjectId).name();
        } catch (BizException ex) {
            return "";
        }
    }

    private BigDecimal accuracyOf(ExamSession session) {
        List<ExamSessionItem> items = itemMapper.selectBySession(session.getId());
        int total = 0;
        int correct = 0;
        for (ExamSessionItem item : items) {
            if (item.getIsCorrect() != null) {
                total++;
                if (Boolean.TRUE.equals(item.getIsCorrect())) {
                    correct++;
                }
            }
        }
        if (total == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return BigDecimal.valueOf(correct).divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP);
    }

    private int usedSecOf(Long sessionId) {
        int used = 0;
        for (ExamSessionSection section : sectionMapper.selectBySession(sessionId)) {
            used += section.getUsedSec() == null ? 0 : section.getUsedSec();
        }
        return used;
    }

    private static List<ExamSessionSection> sectionsOf(List<ExamSessionSection> sections, Integer sittingNo) {
        return sections.stream().filter(s -> s.getSittingNo().equals(sittingNo)).collect(Collectors.toList());
    }

    private static ExamSessionSection findSection(List<ExamSessionSection> sections, int sectionNo) {
        for (ExamSessionSection section : sections) {
            if (section.getSectionNo() != null && section.getSectionNo() == sectionNo) {
                return section;
            }
        }
        return null;
    }

    private static ExamSessionSection nextPendingSection(List<ExamSessionSection> sections,
                                                        int sittingNo, int afterSectionNo) {
        ExamSessionSection candidate = null;
        for (ExamSessionSection section : sections) {
            if (section.getSittingNo() == null || section.getSittingNo() != sittingNo) {
                continue;
            }
            if (section.getSectionNo() == null || section.getSectionNo() <= afterSectionNo) {
                continue;
            }
            if (!answerable(section)) {
                continue;
            }
            if (candidate == null || section.getSectionNo() < candidate.getSectionNo()) {
                candidate = section;
            }
        }
        return candidate;
    }

    private static int firstSectionNo(List<ExamSessionSection> sections, Integer sittingNo) {
        int anySection = Integer.MAX_VALUE;
        int supportedSection = Integer.MAX_VALUE;
        for (ExamSessionSection section : sections) {
            if (section.getSittingNo() == null || !section.getSittingNo().equals(sittingNo)) {
                continue;
            }
            Integer sectionNo = section.getSectionNo();
            if (sectionNo == null) {
                continue;
            }
            anySection = Math.min(anySection, sectionNo);
            if (answerable(section)) {
                supportedSection = Math.min(supportedSection, sectionNo);
            }
        }
        if (supportedSection != Integer.MAX_VALUE) {
            return supportedSection;
        }
        return anySection != Integer.MAX_VALUE ? anySection : 1;
    }

    private static ExamSessionSitting findInProgress(List<ExamSessionSitting> sittings) {
        for (ExamSessionSitting sitting : sittings) {
            if (SittingStatus.IN_PROGRESS.name().equals(sitting.getStatus())) {
                return sitting;
            }
        }
        return null;
    }

    private static ExamSessionSitting findPending(List<ExamSessionSitting> sittings) {
        for (ExamSessionSitting sitting : sittings) {
            if (SittingStatus.PENDING.name().equals(sitting.getStatus())) {
                return sitting;
            }
        }
        return null;
    }

    private static boolean isSupported(PaperSection section) {
        return "SUPPORTED".equals(section.getSupportStatus());
    }

    private static boolean isSupportedSection(ExamSessionSection section) {
        return !SectionStatus.SKIPPED.name().equals(section.getStatus())
                && "SUPPORTED".equals(section.getSupportStatus());
    }

    /**
     * 本节是否有题可答。
     *
     * <p>L4 缩减可能把某个分节缩到 0 题（当前题库没有主观题，所以「应用技术」就是这种）。
     * 一个 0 题的分节不该进入作答流程：否则用户会被要求在"什么都没有的一节"里待满
     * {@code duration_min}（120 分钟）才能离开——一个由组卷降级引发的、完全解释不通的门禁。
     */
    private static boolean answerable(ExamSessionSection section) {
        return isSupportedSection(section)
                && section.getQuestionCount() != null
                && section.getQuestionCount() > 0;
    }

    private static boolean isObjectiveType(String questionType) {
        if (questionType == null) {
            return false;
        }
        try {
            return QuestionType.valueOf(questionType).isObjective();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String serializeOptions(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = new ArrayList<>(options.size());
        for (QuestionOption option : options) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", option.getOptionKey());
            row.put("content", option.getContent());
            rows.add(row);
        }
        return cn.hutool.json.JSONUtil.toJsonStr(rows);
    }

    private static String nextSessionNo(LocalDateTime now) {
        return "EX" + now.format(SESSION_NO_FORMAT) + "-"
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private static int clampDuration(Integer raw) {
        if (raw == null || raw < 0) {
            return 0;
        }
        return Math.min(raw, MAX_DURATION_MS);
    }

    private static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
