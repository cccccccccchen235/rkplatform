package com.ruankao.service.practice.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.PracticePaperRequest;
import com.ruankao.domain.dto.PracticeProgressRequest;
import com.ruankao.domain.dto.PracticeRecordRequest;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.entity.UserAnswerRecord;
import com.ruankao.domain.entity.UserPracticeProgress;
import com.ruankao.domain.enums.PracticeMode;
import com.ruankao.domain.enums.PracticeSource;
import com.ruankao.domain.vo.PracticePaperVO;
import com.ruankao.domain.vo.PracticeProgressVO;
import com.ruankao.domain.vo.PracticeRecordResultVO;
import com.ruankao.domain.vo.QuestionDetailVO;
import com.ruankao.repository.dao.RepeatWindowDao;
import com.ruankao.repository.dao.ReplayGuardDao;
import com.ruankao.repository.mapper.QuestionMapper;
import com.ruankao.repository.mapper.UserAnswerRecordMapper;
import com.ruankao.repository.mapper.UserPracticeProgressMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.practice.JudgeResult;
import com.ruankao.service.practice.JudgeService;
import com.ruankao.service.practice.PracticeService;
import com.ruankao.service.practice.QuestionAssemblyService;
import com.ruankao.service.practice.WrongBookService;
import com.ruankao.service.question.SubjectService;
import com.ruankao.service.stat.StatisticsService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 练习闭环实现（05 §5.5、06 §6.6.3、§6.7、§6.8）。
 */
@Service
public class PracticeServiceImpl implements PracticeService {

    private static final Logger log = LoggerFactory.getLogger(PracticeServiceImpl.class);

    /** 05 §5.5 第 12 条：题量缺省 10。 */
    private static final int DEFAULT_COUNT = 10;

    /** 05 §5.5 第 12 条：{@code SAMPLE} 强制 {@code count ≤ 5}（游客样例题）。 */
    private static final int SAMPLE_MAX_COUNT = 5;

    /**
     * 单题作答耗时的截断上限（04 §4.3 的注释：超过 10 分钟按 10 分钟截断）。
     *
     * <p>不截断的话，"挂机一下午再答一题"会把平均作答时长拉到小时级，
     * 而那个数字会被用于学习行为统计——一个被挂机污染的平均值比没有平均值更糟。
     */
    private static final int MAX_DURATION_MS = 10 * 60 * 1000;

    /** 05 §5.5 第 14 条：{@code outline_id} 的"不按章节"哨兵（见 04 §4.3）。 */
    private static final long OUTLINE_NONE = 0L;

    private final SubjectService subjectService;
    private final QuestionMapper questionMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final UserPracticeProgressMapper userPracticeProgressMapper;
    private final QuestionAssemblyService questionAssemblyService;
    private final JudgeService judgeService;
    private final WrongBookService wrongBookService;
    private final ReplayGuardDao replayGuardDao;
    private final RepeatWindowDao repeatWindowDao;
    private final SysConfigService sysConfigService;
    private final StatisticsService statisticsService;

    public PracticeServiceImpl(SubjectService subjectService,
                               QuestionMapper questionMapper,
                               UserAnswerRecordMapper userAnswerRecordMapper,
                               UserPracticeProgressMapper userPracticeProgressMapper,
                               QuestionAssemblyService questionAssemblyService,
                               JudgeService judgeService,
                               WrongBookService wrongBookService,
                               ReplayGuardDao replayGuardDao,
                               RepeatWindowDao repeatWindowDao,
                               SysConfigService sysConfigService,
                               StatisticsService statisticsService) {
        this.subjectService = subjectService;
        this.questionMapper = questionMapper;
        this.userAnswerRecordMapper = userAnswerRecordMapper;
        this.userPracticeProgressMapper = userPracticeProgressMapper;
        this.questionAssemblyService = questionAssemblyService;
        this.judgeService = judgeService;
        this.wrongBookService = wrongBookService;
        this.replayGuardDao = replayGuardDao;
        this.repeatWindowDao = repeatWindowDao;
        this.sysConfigService = sysConfigService;
        this.statisticsService = statisticsService;
    }

    // ------------------------------------------------------------------
    // 组卷
    // ------------------------------------------------------------------

    @Override
    public PracticePaperVO generatePaper(Long userId, PracticePaperRequest request) {
        Long subjectId = requireId(request.getSubjectId(), "科目 ID 非法");
        String mode = request.getMode();
        boolean guest = userId == null;
        boolean sample = PracticeMode.SAMPLE.name().equals(mode);

        // 游客资格：只有样例题放行，其余模式一律 2005（评审 B6，05 §5.13）
        if (guest && !sample) {
            throw new BizException(ErrorCode.NOT_LOGGED_IN);
        }

        int count = resolveCount(request.getCount(), sample);

        // 科目必须存在且启用（3001）。复用 SubjectService.getDetail 而不是自己查库：
        // "不存在"与"已停用"合并成 3001 这条规则只有一处实现，不能在这里再写一份。
        subjectService.getDetail(subjectId);

        // 游客没有掌握度画像，onlyUnmastered 对他们无意义（且 SQL 里 user_id 为 NULL
        // 会让排除条件恒不命中），直接忽略而不是报错。
        boolean onlyUnmastered = !guest && Boolean.TRUE.equals(request.getOnlyUnmastered());
        Integer difficulty = request.getDifficulty();
        Long refId = parseOptionalId(request.getRefId());

        // onlyUnmastered 直接读 stat_user_kp.mastery；先按脏标记把可能过期的掌握度重算掉，
        // 否则这一卷用的还是上一轮的画像（06 §6.10.2 的"读取时重算"）。
        // 只在开了该开关时触发：没有掌握度过滤的组卷不需要为统计付出一次重算。
        if (onlyUnmastered) {
            statisticsService.refreshIfStale(userId, subjectId);
        }

        List<Question> questions = pickQuestions(mode, subjectId, userId, refId,
                request.getFromSequence(), onlyUnmastered, difficulty, count);

        List<QuestionDetailVO> vos = questionAssemblyService.toDetailList(userId, questions);
        return new PracticePaperVO(vos, vos.size(), resolveNextSequence(mode, questions));
    }

    /**
     * 归一化题量。
     *
     * <p>{@code SAMPLE} 缺省按 5 而不是 10：游客不传 count 时若落到默认 10，
     * 会立刻撞上"样例题最多 5 道"的 1001——一个"什么都不传"的请求被拒，
     * 对调用方是纯粹的困惑。默认值应当落在合法区间内。
     */
    private static int resolveCount(Integer raw, boolean sample) {
        if (raw == null) {
            return sample ? SAMPLE_MAX_COUNT : DEFAULT_COUNT;
        }
        if (sample && raw > SAMPLE_MAX_COUNT) {
            throw new BizException(ErrorCode.PARAM_INVALID, "样例题最多 " + SAMPLE_MAX_COUNT + " 道");
        }
        return raw;
    }

    /**
     * 按模式取候选池（06 §6.6.3）。
     *
     * <p>六种模式各自一个 mapper 方法而不是一条动态 SQL：它们的 FROM/JOIN/ORDER 都不同，
     * 硬塞进一个 {@code <choose>} 会让"某个模式的条件写错了"极难看出。
     * 但它们共享 {@code publishedPredicate}（AC-5）与 {@code poolFilters}（掌握度/难度）。
     */
    private List<Question> pickQuestions(String mode, Long subjectId, Long userId, Long refId,
                                         Integer fromSequence, boolean onlyUnmastered,
                                         Integer difficulty, int count) {
        if (PracticeMode.CHAPTER.name().equals(mode)) {
            if (refId == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "章节练习必须指定章节");
            }
            List<Long> outlineIds = questionAssemblyService.outlineSubtreeIds(subjectId, refId);
            return questionMapper.selectRandomByOutlines(subjectId, outlineIds, userId,
                    onlyUnmastered, difficulty, count);
        }
        if (PracticeMode.KP.name().equals(mode)) {
            if (refId == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "知识点练习必须指定知识点");
            }
            return questionMapper.selectRandomByKp(subjectId, refId, userId,
                    onlyUnmastered, difficulty, count);
        }
        if (PracticeMode.SEQUENCE.name().equals(mode)) {
            // fromSequence 缺省 = 从头开始；转成 Long 让 SQL 的 q.id > ? 成立
            Long from = fromSequence == null ? null : fromSequence.longValue();
            return questionMapper.selectBySequence(subjectId, from, count);
        }
        if (PracticeMode.FAVORITE.name().equals(mode)) {
            return questionMapper.selectRandomByFavorite(userId, subjectId,
                    onlyUnmastered, difficulty, count);
        }
        if (PracticeMode.WRONG_REDO.name().equals(mode)) {
            return questionMapper.selectRandomByWrong(userId, subjectId,
                    onlyUnmastered, difficulty, count);
        }
        if (PracticeMode.SAMPLE.name().equals(mode)) {
            return questionMapper.selectRandomBySubject(subjectId, userId,
                    onlyUnmastered, difficulty, count);
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "练习模式非法");
    }

    /**
     * 算续做游标（05 §5.5 第 12 条的 {@code nextSequence}）。
     *
     * <p>只有 {@code SEQUENCE} 有游标语义，其余模式恒返回 0：给随机模式返回"最后一题的 ID"
     * 会让前端误以为可以拿它续做，而这些模式每次取的都是随机子集，续做没有意义。
     *
     * <p>{@code q.id} 是 {@code BIGINT} 而契约里游标是 {@code int}（04 的
     * {@code last_sequence INT}）：这里用 {@code Math.toIntExact} 而不是强转——
     * 该设计本身假设题序放得进 int，若真有溢出，宁可让它在组卷时明确失败，
     * 也不要静默截断出一个会让用户"继续上次练习"跳到错误位置的值。
     */
    private static int resolveNextSequence(String mode, List<Question> questions) {
        if (!PracticeMode.SEQUENCE.name().equals(mode) || questions.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(questions.get(questions.size() - 1).getId());
    }

    // ------------------------------------------------------------------
    // 上报
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public PracticeRecordResultVO report(Long userId, PracticeRecordRequest request) {
        Long subjectId = requireId(request.getSubjectId(), "科目 ID 非法");
        subjectService.getDetail(subjectId);

        // 先按 ID 把题从库里取回来：题干的权威版本在服务端，判分只认它（AC-3）
        List<Long> questionIds = new ArrayList<>(request.getItems().size());
        for (PracticeRecordRequest.Item item : request.getItems()) {
            questionIds.add(requireId(item.getQuestionId(), "题目 ID 非法"));
        }
        Map<Long, Question> byId = new LinkedHashMap<>();
        for (Question question : questionMapper.selectByIds(questionIds)) {
            byId.put(question.getId(), question);
        }

        LocalDateTime now = LocalDateTime.now();
        int accepted = 0;
        int correctCount = 0;
        int durationMsSum = 0;
        List<Long> acceptedIds = new ArrayList<>(request.getItems().size());

        for (PracticeRecordRequest.Item item : request.getItems()) {
            Long questionId = requireId(item.getQuestionId(), "题目 ID 非法");
            Question question = byId.get(questionId);

            // 题目不存在、或不属于本次声明的科目：跳过并留痕。
            // 不整批 400：上报是可重试的（验收项「恢复网络后记录补传成功」），
            // 一条脏数据让整批反复失败会让客户端陷入无限重试；
            // 而 accepted 字段本来就是为"这一批里真正落库了几条"准备的。
            if (question == null || !subjectId.equals(question.getSubjectId())) {
                log.warn("答题上报跳过：题目不存在或不属于该科目。userId={}, questionId={}, subjectId={}",
                        userId, questionId, subjectId);
                continue;
            }

            // 5 秒重放闸（05 §5.5 第 13 条）：重放不算 accepted，也不重复计对错
            if (!replayGuardDao.tryAcquire(userId, questionId)) {
                log.debug("答题上报忽略重放：userId={}, questionId={}", userId, questionId);
                continue;
            }

            JudgeResult judged = judgeService.judge(question, item.getUserAnswer());

            UserAnswerRecord record = new UserAnswerRecord();
            record.setUserId(userId);
            record.setSubjectId(subjectId);
            record.setOutlineId(question.getOutlineId());
            record.setQuestionId(questionId);
            record.setSource(request.getSource());
            // session_id 仅 source=EXAM 时有值（04 §4.3），练习恒为 null
            record.setSessionId(null);
            // 客观题存选项 key；主观题按 DDL 注释留空（不判分，原文没有统计价值）
            record.setUserAnswer(QuestionAssemblyService.hasOptions(question.getQuestionType())
                    ? normalizeAnswer(item.getUserAnswer())
                    : null);
            record.setIsCorrect(judged.isCorrect());
            record.setDurationMs(clampDuration(item.getDurationMs()));
            record.setAnsweredAt(now);
            userAnswerRecordMapper.insert(record);

            wrongBookService.onAnswered(userId, questionId, subjectId, judged);

            accepted++;
            durationMsSum += record.getDurationMs();
            acceptedIds.add(questionId);
            if (Boolean.TRUE.equals(judged.isCorrect())) {
                correctCount++;
            }
        }

        if (!acceptedIds.isEmpty()) {
            // 防重复窗口：写方在练习，读方在模拟考（06 §6.6.4）
            repeatWindowDao.add(userId, subjectId, acceptedIds,
                    Duration.ofDays(sysConfigService.repeatWindowDays()));
            recordSequenceProgress(userId, subjectId, request.getSource(), accepted, correctCount);

            // 统计计数与流水同事务（06 §6.4）：两者要么一起成功、要么一起回滚，
            // 不会留下"答了题但当天趋势少一笔"这种需要人工对齐的中间态。
            statisticsService.incrementCounters(userId, subjectId, accepted, correctCount,
                    durationMsSum / 1000, now);

            // 掌握度重算放到提交之后（06 §6.10.2）：重算读的是已提交的流水，
            // 在事务内重算既读不到本批数据，又会把上报事务拉长到一次聚合查询的耗时。
            markStatisticsStaleAfterCommit(userId, subjectId);
        }

        return new PracticeRecordResultVO(accepted, correctCount);
    }

    /**
     * 事务提交后点亮统计脏标记（06 §6.10.2）。
     *
     * <p>用 {@code afterCommit} 而不是在方法体末尾直接写：{@link #report} 标了
     * {@code @Transactional}，方法体末尾仍在事务内——此时置标记，一旦随后回滚，
     * 标记就指向一批并不存在的数据，触发一次永远"重算不出新东西"的无效重算。
     * 没有事务同步时（例如单测直接调用）退化为同步写入，不抛异常。
     */
    private void markStatisticsStaleAfterCommit(Long userId, Long subjectId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            statisticsService.markStale(userId, subjectId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                statisticsService.markStale(userId, subjectId);
            }
        });
    }

    /**
     * 顺手累加"顺序练习"的进度计数（05 §5.5 第 14 条的 {@code answeredCount}）。
     *
     * <p>只对 {@code PRACTICE_SEQUENCE} 累加：其余模式没有游标语义（每次取的都是随机子集），
     * 给它们记一个"作答题数"并不能回答"我练到哪了"，反而会让进度接口看起来有数据却没法用。
     *
     * <p>{@code outline_id} 用 {@link #OUTLINE_NONE}：上报请求不带章节 refId，
     * 而 {@code user_practice_progress} 的章节维度只对"按章节练习的游标"有意义。
     */
    private void recordSequenceProgress(Long userId, Long subjectId, String source,
                                        int accepted, int correctCount) {
        if (!PracticeSource.PRACTICE_SEQUENCE.name().equals(source)) {
            return;
        }
        userPracticeProgressMapper.incrementCounts(userId, subjectId,
                PracticeMode.SEQUENCE.name(), OUTLINE_NONE, accepted, correctCount);
    }

    private static String normalizeAnswer(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int clampDuration(Integer raw) {
        if (raw == null || raw < 0) {
            return 0;
        }
        return Math.min(raw, MAX_DURATION_MS);
    }

    // ------------------------------------------------------------------
    // 进度
    // ------------------------------------------------------------------

    @Override
    public List<PracticeProgressVO> getProgress(Long userId, Long subjectId, String mode) {
        if (subjectId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "科目 ID 不能为空");
        }
        subjectService.getDetail(subjectId);

        List<UserPracticeProgress> rows;
        if (mode == null || mode.isBlank()) {
            rows = userPracticeProgressMapper.selectBySubject(userId, subjectId);
        } else {
            String normalized = normalizeProgressMode(mode);
            UserPracticeProgress row =
                    userPracticeProgressMapper.selectOne(userId, subjectId, normalized, OUTLINE_NONE);
            rows = row == null ? List.of() : List.of(row);
        }

        List<PracticeProgressVO> result = new ArrayList<>(rows.size());
        for (UserPracticeProgress row : rows) {
            result.add(toProgressVO(row, subjectId, userId));
        }
        return result;
    }

    private PracticeProgressVO toProgressVO(UserPracticeProgress row, Long subjectId, Long userId) {
        String practiceMode = row.getPracticeType();
        Long refId = row.getOutlineId() == null || row.getOutlineId() == OUTLINE_NONE
                ? null
                : row.getOutlineId();
        // 分母与实际能取到的题共用同一批 SQL 片段（见 QuestionMapper.countPracticePool）
        long total = questionMapper.countPracticePool(
                subjectId,
                practiceMode,
                refId,
                PracticeMode.CHAPTER.name().equals(practiceMode) && refId != null
                        ? questionAssemblyService.outlineSubtreeIds(subjectId, refId)
                        : null,
                userId,
                false,
                null);

        return new PracticeProgressVO(
                String.valueOf(subjectId),
                practiceMode,
                refId == null ? null : String.valueOf(refId),
                row.getLastSequence(),
                row.getLastQuestionId() == null ? null : String.valueOf(row.getLastQuestionId()),
                Math.toIntExact(total),
                row.getAnsweredCount(),
                row.getUpdatedAt());
    }

    @Override
    public void saveProgress(Long userId, PracticeProgressRequest request) {
        Long subjectId = requireId(request.getSubjectId(), "科目 ID 非法");
        subjectService.getDetail(subjectId);

        String mode = normalizeProgressMode(request.getMode());
        Long refId = parseOptionalId(request.getRefId());
        long outlineId = refId == null ? OUTLINE_NONE : refId;

        // 请求体只带游标，不带计数；而已有行里的计数是答题上报累加出来的。
        // 先读出来再原样写回，避免一次"继续练习"把累计作答数抹成 0。
        UserPracticeProgress existing =
                userPracticeProgressMapper.selectOne(userId, subjectId, mode, outlineId);

        UserPracticeProgress progress = new UserPracticeProgress();
        progress.setUserId(userId);
        progress.setSubjectId(subjectId);
        progress.setPracticeType(mode);
        progress.setOutlineId(outlineId);
        progress.setLastSequence(request.getLastSequence());
        progress.setLastQuestionId(parseOptionalId(request.getLastQuestionId()));
        progress.setAnsweredCount(existing == null ? 0 : existing.getAnsweredCount());
        progress.setCorrectCount(existing == null ? 0 : existing.getCorrectCount());
        userPracticeProgressMapper.upsert(progress);
    }

    /**
     * 归一化进度接口的 mode。
     *
     * <p>不含 {@code SAMPLE}：游客没有持久化进度（05 §5.5 第 15 条列的是
     * {@code CHAPTER/KP/SEQUENCE/FAVORITE}，第 14 条补了 {@code WRONG_REDO}）。
     */
    private static String normalizeProgressMode(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "练习模式不能为空");
        }
        String upper = mode.trim().toUpperCase();
        try {
            PracticeMode parsed = PracticeMode.valueOf(upper);
            if (PracticeMode.SAMPLE == parsed) {
                throw new BizException(ErrorCode.PARAM_INVALID, "样例题没有练习进度");
            }
            return parsed.name();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "练习模式非法");
        }
    }

    // ------------------------------------------------------------------

    /** 解析必填 ID（字符串 → Long）。非法值报 1001，不静默当成 0。 */
    private static Long requireId(String raw, String message) {
        Long id = parseOptionalId(raw);
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
        return id;
    }

    /** 解析可选 ID：{@code null}/空白 → {@code null}；非数字 → 1001。 */
    private static Long parseOptionalId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "ID 必须是数字");
        }
    }
}
