package com.ruankao.service.exam;

import com.ruankao.domain.entity.ExamSession;
import com.ruankao.domain.entity.ExamSessionSitting;
import com.ruankao.domain.enums.ExamStatus;
import com.ruankao.repository.mapper.ExamSessionMapper;
import com.ruankao.repository.mapper.ExamSessionSittingMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.exam.impl.ExamTimeoutSettler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模拟考的定时兜底（06 §6.9.2 第 2 条、§6.13）。
 *
 * <p>两个任务都在"惰性结算"之外补漏：请求路径上的结算只在有人访问该会话时发生，
 * 而"考完再也不回来"的会话永远不会被访问。
 *
 * <p>调度器只做扫描与去重，结算本身交给 {@link ExamTimeoutSettler}（独立事务，
 * 单个会话失败不影响其他会话）。
 */
@Component
public class ExamSessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionScheduler.class);

    private static final int EXPIRED_LIMIT = 500;
    private static final int STALE_LIMIT = 200;
    /** 06 §6.13：上一场次结束超过 N 小时仍未开始下一场次 → 自动交卷。 */
    private static final int STALE_HOURS = 6;

    private final ExamSessionSittingMapper sittingMapper;
    private final ExamSessionMapper sessionMapper;
    private final ExamTimeoutSettler timeoutSettler;
    private final SysConfigService sysConfigService;

    public ExamSessionScheduler(ExamSessionSittingMapper sittingMapper,
                                ExamSessionMapper sessionMapper,
                                ExamTimeoutSettler timeoutSettler,
                                SysConfigService sysConfigService) {
        this.sittingMapper = sittingMapper;
        this.sessionMapper = sessionMapper;
        this.timeoutSettler = timeoutSettler;
        this.sysConfigService = sysConfigService;
    }

    /** 每 5 分钟：结算已超过时长的进行中场次。 */
    @Scheduled(cron = "0 */5 * * * *")
    public void settleExpiredSittings() {
        if (!sysConfigService.examTimeoutEnabled()) {
            return;
        }
        Set<Long> handled = new HashSet<>();
        int settled = 0;
        for (ExamSessionSitting sitting : sittingMapper.selectExpired(EXPIRED_LIMIT)) {
            if (!handled.add(sitting.getSessionId())) {
                continue;
            }
            ExamSession session = sessionMapper.selectById(sitting.getSessionId());
            if (session == null || !ExamStatus.IN_PROGRESS.name().equals(session.getStatus())) {
                continue;
            }
            try {
                if (timeoutSettler.settleExpired(session.getUserId(), session.getId())) {
                    settled++;
                }
            } catch (Exception ex) {
                log.warn("超时结算失败：sessionId={}", session.getId(), ex);
            }
        }
        if (settled > 0) {
            log.info("模拟考超时结算完成：本批结算 {} 个会话", settled);
        }
    }

    /** 每 30 分钟：结算"场次一结束、场次二迟迟不开始"的滞留会话（评审 G1）。 */
    @Scheduled(cron = "0 */30 * * * *")
    public void settleStaleSessions() {
        if (!sysConfigService.sessionStaleEnabled()) {
            return;
        }
        Set<Long> handled = new HashSet<>();
        int settled = 0;
        for (ExamSessionSitting sitting : sittingMapper.selectStalePending(STALE_HOURS, STALE_LIMIT)) {
            if (!handled.add(sitting.getSessionId())) {
                continue;
            }
            ExamSession session = sessionMapper.selectById(sitting.getSessionId());
            if (session == null || !ExamStatus.IN_PROGRESS.name().equals(session.getStatus())) {
                continue;
            }
            try {
                if (timeoutSettler.settleStale(session.getUserId(), session.getId())) {
                    settled++;
                }
            } catch (Exception ex) {
                log.warn("会话级滞留结算失败：sessionId={}", session.getId(), ex);
            }
        }
        if (settled > 0) {
            log.info("会话级滞留结算完成：本批结算 {} 个会话", settled);
        }
    }
}
