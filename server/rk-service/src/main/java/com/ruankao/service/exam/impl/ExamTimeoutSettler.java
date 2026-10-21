package com.ruankao.service.exam.impl;

import com.ruankao.domain.entity.ExamSession;
import com.ruankao.domain.entity.ExamSessionSitting;
import com.ruankao.domain.enums.ExamStatus;
import com.ruankao.domain.enums.SittingStatus;
import com.ruankao.repository.mapper.ExamSessionMapper;
import com.ruankao.repository.mapper.ExamSessionSectionMapper;
import com.ruankao.repository.mapper.ExamSessionSittingMapper;
import com.ruankao.service.exam.ExamScoringService;
import com.ruankao.service.exam.ExamTiming;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 超时结算（06 §6.9.2）：把"该结算"这件事做成**可独立提交**的一个事务。
 *
 * <p>为什么必须 {@code REQUIRES_NEW}：结算之后调用方要按契约返回 {@code 4005}，
 * 而 {@code BizException} 会让外层事务回滚——如果结算写在外层事务里，它会被一起回滚掉。
 * 独立事务提交后，外层即使抛异常，结算结果也已落库。
 */
@Service
public class ExamTimeoutSettler {

    private static final Logger log = LoggerFactory.getLogger(ExamTimeoutSettler.class);

    private final ExamSessionMapper sessionMapper;
    private final ExamSessionSittingMapper sittingMapper;
    private final ExamSessionSectionMapper sectionMapper;
    private final ExamScoringService examScoringService;

    public ExamTimeoutSettler(ExamSessionMapper sessionMapper,
                              ExamSessionSittingMapper sittingMapper,
                              ExamSessionSectionMapper sectionMapper,
                              ExamScoringService examScoringService) {
        this.sessionMapper = sessionMapper;
        this.sittingMapper = sittingMapper;
        this.sectionMapper = sectionMapper;
        this.examScoringService = examScoringService;
    }

    /**
     * 若当前场次已耗尽则结算它。有下一场次则停下等用户主动开始；已是最后一场则整卷交卷。
     *
     * @return true 表示本次确实结算了（调用方据此返回 4005）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean settleExpired(Long userId, Long sessionId) {
        ExamSession session = sessionMapper.selectByIdAndUser(sessionId, userId);
        if (session == null || !ExamStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return false;
        }
        ExamSessionSitting current = findInProgress(sittingMapper.selectBySession(sessionId));
        if (current == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!ExamTiming.exhausted(current.getStartedAt(), current.getTotalDurationSec(), now)) {
            return false;
        }

        sittingMapper.finish(current.getId(), current.getObjectiveScore(), now);
        sectionMapper.finishSitting(sessionId, current.getSittingNo(), now);

        ExamSessionSitting next = findPending(sittingMapper.selectBySession(sessionId));
        if (next != null) {
            sessionMapper.updateProgress(sessionId, ExamStatus.IN_PROGRESS.name(),
                    next.getSittingNo(), next.getCurrentSectionNo(), 0, now);
            log.info("场次超时结束，等待用户主动开始下一场次：sessionId={}, sittingNo={}",
                    sessionId, current.getSittingNo());
        } else {
            log.info("最后一场次超时，自动交卷：sessionId={}", sessionId);
            examScoringService.score(userId, sessionId);
        }
        return true;
    }

    /**
     * 会话级滞留（评审 G1）：用户考完上午连考再也不回来，会话停在 IN_PROGRESS、场次二停在 PENDING。
     * 直接整卷交卷——保留场次一已考出的成绩，未作答的场次二在成绩页标注"未作答"。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean settleStale(Long userId, Long sessionId) {
        ExamSession session = sessionMapper.selectByIdAndUser(sessionId, userId);
        if (session == null || !ExamStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return false;
        }
        log.info("会话级滞留结算：sessionId={}, userId={}", sessionId, userId);
        examScoringService.score(userId, sessionId);
        return true;
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
}
