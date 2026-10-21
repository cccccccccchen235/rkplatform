package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.PracticePaperRequest;
import com.ruankao.domain.dto.PracticeProgressRequest;
import com.ruankao.domain.dto.PracticeRecordRequest;
import com.ruankao.domain.vo.PracticePaperVO;
import com.ruankao.domain.vo.PracticeProgressVO;
import com.ruankao.domain.vo.PracticeRecordResultVO;
import com.ruankao.service.practice.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 练习（05 §5.5 第 12~15 条）。
 *
 * <p>四个方法里只有 {@code POST /practice/papers} 允许游客——它带着 {@code @SaIgnore}，
 * 而游客资格（{@code mode = SAMPLE} 且 {@code count ≤ 5}）在方法体内校验。
 * <b>该路径刻意不在 {@code AuthWhitelist} 里</b>：白名单只能表达"整条路径放行"，
 * 一旦写入，游客就能用 {@code mode = FULL} 绕过题量限制（评审 B6，安全洞）。
 * {@code AuthAnnotationTest} 会断言它不在白名单中。
 *
 * <p>其余三个方法各自标注 {@code @SaCheckLogin}（逐个方法标而不是类级：
 * 类级注解会把 {@code /papers} 也一起要求登录，恰好抹掉这个接口的游客能力）。
 */
@RestController
@RequestMapping("/api/v1/practice")
@Validated
@Tag(name = "练习", description = "05 §5.5：练习组卷、答题上报、练习进度")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    /**
     * 生成练习卷（05 §5.5 第 12 条）。
     *
     * <p>游客可用，但仅限样例题：未登录时 {@code mode} 必须是 {@code SAMPLE} 且
     * {@code count ≤ 5}，否则 2005 / 1001（由 Service 判定，见方法注释）。
     */
    @PostMapping("/papers")
    @SaIgnore
    @Operation(summary = "生成练习卷",
            description = "练习模式随题目一起下发答案，由前端即时判分；上报时服务端会重新判分。"
                    + "游客仅可 mode=SAMPLE 且 count<=5。")
    public R<PracticePaperVO> createPaper(@Valid @RequestBody PracticePaperRequest request) {
        return R.ok(practiceService.generatePaper(currentUserIdOrNull(), request));
    }

    /**
     * 批量上报答题结果（05 §5.5 第 13 条）。
     *
     * <p>请求体里没有"对错"字段，服务端会用 {@code JudgeService} 重新判分（AC-3）。
     */
    @PostMapping("/records")
    @SaCheckLogin
    @Operation(summary = "批量上报答题结果",
            description = "服务端重新判分后落库；同一题目 5 秒内的重复上报视为重放忽略。")
    public R<PracticeRecordResultVO> report(@Valid @RequestBody PracticeRecordRequest request) {
        return R.ok(practiceService.report(StpUtil.getLoginIdAsLong(), request));
    }

    /** 查练习进度（05 §5.5 第 14 条）。缺省 {@code mode} 时返回该科目全部模式的游标。 */
    @GetMapping("/progress")
    @SaCheckLogin
    @Operation(summary = "查练习进度", description = "恒返回数组；不传 mode 时返回该科目全部模式。")
    public R<List<PracticeProgressVO>> getProgress(
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(name = "mode", required = false) String mode) {
        return R.ok(practiceService.getProgress(StpUtil.getLoginIdAsLong(), subjectId, mode));
    }

    /** 更新练习游标（05 §5.5 第 15 条）。幂等 upsert。 */
    @PutMapping("/progress")
    @SaCheckLogin
    @Operation(summary = "更新练习进度", description = "幂等 upsert；不覆盖已累计的作答题数。")
    public R<Void> saveProgress(@Valid @RequestBody PracticeProgressRequest request) {
        practiceService.saveProgress(StpUtil.getLoginIdAsLong(), request);
        return R.ok();
    }

    /**
     * 取当前登录用户 ID，未登录返回 {@code null}。
     *
     * <p>不能用 {@code StpUtil.getLoginIdAsLong()}：它在未登录时抛异常，
     * 而 {@code /papers} 恰恰允许未登录——那会把一次合法的样例题请求变成 500。
     */
    private static Long currentUserIdOrNull() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
    }
}