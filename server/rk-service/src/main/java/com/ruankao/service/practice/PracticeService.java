package com.ruankao.service.practice;

import com.ruankao.domain.dto.PracticePaperRequest;
import com.ruankao.domain.dto.PracticeProgressRequest;
import com.ruankao.domain.dto.PracticeRecordRequest;
import com.ruankao.domain.vo.PracticePaperVO;
import com.ruankao.domain.vo.PracticeProgressVO;
import com.ruankao.domain.vo.PracticeRecordResultVO;
import java.util.List;

/**
 * 练习闭环（05 §5.5 第 12~15 条）。
 *
 * <p>四种模式的差别只有"从哪里取题"，其余（下发答案、前端即时判分、上报时服务端复核、
 * 错题本、进度）全部共用同一条路径。因此本接口只有四个方法，mode 的作用域被收在
 * 组卷内部的一个 {@code switch} 里——不是四个 Service。
 */
public interface PracticeService {

    /**
     * 生成练习卷（05 §5.5 第 12 条）。
     *
     * <p><b>本方法允许游客调用，但资格在方法体内判定</b>（评审 B6 的安全洞）：
     * 仅 {@code mode = SAMPLE} 且 {@code count ≤ 5} 时放行，否则 {@code 2005} / {@code 1001}。
     * 这条判断不能上移到拦截器白名单——白名单只能表达"整条路径放行"，
     * 一旦写入，游客就能用 {@code mode = FULL} 绕过题量限制。
     *
     * @param userId 当前登录用户；游客为 {@code null}
     */
    PracticePaperVO generatePaper(Long userId, PracticePaperRequest request);

    /**
     * 批量上报答题结果（05 §5.5 第 13 条）。
     *
     * <p><b>服务端重新判分</b>（AC-3）：请求体里没有 {@code isCorrect}，
     * 对错由 {@code JudgeService} 依据服务端取到的题目重算，
     * 客户端报什么都不影响落库结果。
     *
     * <p>整体一个事务：写流水 + 更新错题本。任一环节失败则整批回滚，
     * 避免"记录写了一半、错题本更新了一半"这种需要人工对齐的中间态。
     */
    PracticeRecordResultVO report(Long userId, PracticeRecordRequest request);

    /**
     * 查练习进度（05 §5.5 第 14 条）。
     *
     * <p>恒返回**列表**：契约写明"缺省 mode 时返回该科目全部模式的游标"，
     * 而单个模式只是它的一个子集。若 mode 给定时返回裸对象、不给定时返回数组，
     * 前端就要写两套解析分支。
     *
     * @param mode 为 {@code null} 时返回全部模式
     */
    List<PracticeProgressVO> getProgress(Long userId, Long subjectId, String mode);

    /** 更新顺序练习游标（05 §5.5 第 15 条）。幂等 upsert，不覆盖已有计数。 */
    void saveProgress(Long userId, PracticeProgressRequest request);
}