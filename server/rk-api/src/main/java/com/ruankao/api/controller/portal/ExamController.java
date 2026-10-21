package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.api.PageResult;
import com.ruankao.common.api.R;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.ExamAnswerRequest;
import com.ruankao.domain.dto.ExamCreateRequest;
import com.ruankao.domain.vo.ExamAnswerResultVO;
import com.ruankao.domain.vo.ExamHistoryVO;
import com.ruankao.domain.vo.ExamResultVO;
import com.ruankao.domain.vo.ExamSectionAdvanceVO;
import com.ruankao.domain.vo.ExamSessionVO;
import com.ruankao.domain.vo.ExamSittingFinishVO;
import com.ruankao.domain.vo.ExamSittingStartVO;
import com.ruankao.service.exam.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟考（05 §5.6 第 16~25 条）。
 *
 * <p>十个接口全部 {@code USER} 权限，故 {@code @SaCheckLogin} 标在类上。
 * 计时、状态流转与超时结算都在服务层，控制器只做参数搬运。
 */
@RestController
@RequestMapping("/api/v1/exams")
@SaCheckLogin
@Validated
@Tag(name = "模拟考", description = "05 §5.6：组卷、答题、场次/分节、交卷、成绩")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /** 发起模拟考（05 §5.6 第 16 条）。有未完成会话返回 4003。 */
    @PostMapping
    @Operation(summary = "发起模拟考", description = "组卷并创建会话；返回不含答案的整卷。")
    public R<ExamSessionVO> create(@Valid @RequestBody ExamCreateRequest request) {
        return R.ok(examService.create(StpUtil.getLoginIdAsLong(), parseId(request.getSubjectId())));
    }

    /** 历史成绩（05 §5.6 第 17 条）。默认不含 ABANDONED，永不含 IN_PROGRESS。 */
    @GetMapping
    @Operation(summary = "历史成绩列表", description = "默认只返回 SUBMITTED/SCORED；IN_PROGRESS 永不返回。")
    public R<PageResult<ExamHistoryVO>> history(
            @RequestParam(name = "subjectId", required = false) String subjectId,
            @RequestParam(name = "includeAbandoned", required = false, defaultValue = "false") boolean includeAbandoned,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {
        return R.ok(examService.history(StpUtil.getLoginIdAsLong(), parseId(subjectId),
                includeAbandoned, pageNum, pageSize));
    }

    /** 会话详情（05 §5.6 第 18 条）。断点续答入口。 */
    @GetMapping("/{id}")
    @Operation(summary = "会话详情", description = "刷新/换设备后据此恢复已答内容与剩余时间。")
    public R<ExamSessionVO> get(@PathVariable("id") Long id) {
        return R.ok(examService.get(StpUtil.getLoginIdAsLong(), id));
    }

    /** 保存作答（05 §5.6 第 19 条）。空 items 即心跳。 */
    @PutMapping("/{id}/answers")
    @Operation(summary = "保存作答", description = "不判分；items 为空数组时退化为心跳与计时校准。")
    public R<ExamAnswerResultVO> saveAnswers(@PathVariable("id") Long id,
                                             @Valid @RequestBody ExamAnswerRequest request) {
        return R.ok(examService.saveAnswers(StpUtil.getLoginIdAsLong(), id, request));
    }

    /** 启动场次计时（05 §5.6 第 20 条）。场次二论文由用户主动开始。 */
    @PostMapping("/{id}/sittings/{sittingNo}/start")
    @Operation(summary = "启动场次计时", description = "PENDING → IN_PROGRESS；场次之间不自动衔接。")
    public R<ExamSittingStartVO> startSitting(@PathVariable("id") Long id,
                                              @PathVariable("sittingNo") int sittingNo) {
        return R.ok(examService.startSitting(StpUtil.getLoginIdAsLong(), id, sittingNo));
    }

    /** 离开当前分节（05 §5.6 第 21 条）。 */
    @PostMapping("/{id}/sections/{sectionNo}/advance")
    @Operation(summary = "离开当前分节", description = "需满足 durationMin；最后一节等价于结束本场次。")
    public R<ExamSectionAdvanceVO> advanceSection(@PathVariable("id") Long id,
                                                  @PathVariable("sectionNo") int sectionNo) {
        return R.ok(examService.advanceSection(StpUtil.getLoginIdAsLong(), id, sectionNo));
    }

    /** 提前结束场次（05 §5.6 第 22 条）。 */
    @PostMapping("/{id}/sittings/{sittingNo}/finish")
    @Operation(summary = "提前结束场次", description = "论文节不允许提前结束。")
    public R<ExamSittingFinishVO> finishSitting(@PathVariable("id") Long id,
                                                @PathVariable("sittingNo") int sittingNo) {
        return R.ok(examService.finishSitting(StpUtil.getLoginIdAsLong(), id, sittingNo));
    }

    /** 交卷（05 §5.6 第 23 条）。幂等。 */
    @PostMapping("/{id}/submit")
    @Operation(summary = "交卷", description = "全部分节已作答或超时；幂等，连点两次只结算一次。")
    public R<ExamResultVO> submit(@PathVariable("id") Long id) {
        return R.ok(examService.submit(StpUtil.getLoginIdAsLong(), id));
    }

    /** 成绩详情（05 §5.6 第 24 条）。此时才返回答案与解析。 */
    @GetMapping("/{id}/result")
    @Operation(summary = "成绩详情", description = "含答案与解析；未交卷返回 1005。")
    public R<ExamResultVO> result(@PathVariable("id") Long id) {
        return R.ok(examService.result(StpUtil.getLoginIdAsLong(), id));
    }

    /** 放弃（05 §5.6 第 25 条）。不计入模考统计。 */
    @PostMapping("/{id}/abandon")
    @Operation(summary = "放弃会话", description = "置 ABANDONED，不计入成绩统计。")
    public R<Void> abandon(@PathVariable("id") Long id) {
        examService.abandon(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    private static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "科目 ID 必须是数字");
        }
    }
}
