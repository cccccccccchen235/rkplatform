package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.PageQuery;
import com.ruankao.common.api.PageResult;
import com.ruankao.common.api.R;
import com.ruankao.domain.vo.WrongQuestionVO;
import com.ruankao.service.practice.WrongBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错题本（05 §5.7 第 26~28 条）。
 *
 * <p>三个方法都要登录：错题本按 {@code user_id} 隔离，游客没有可读的错题。
 */
@RestController
@RequestMapping("/api/v1/wrong-questions")
@Validated
@Tag(name = "错题本", description = "05 §5.7：错题列表、移出、恢复")
public class WrongQuestionController {

    private final WrongBookService wrongBookService;

    public WrongQuestionController(WrongBookService wrongBookService) {
        this.wrongBookService = wrongBookService;
    }

    /** 错题列表（05 §5.7 第 26 条）。{@code status} 缺省为 {@code ACTIVE}。 */
    @GetMapping
    @SaCheckLogin
    @Operation(summary = "错题列表", description = "status 缺省 ACTIVE；sortBy 支持 wrongCount / lastAnsweredAt。")
    public R<PageResult<WrongQuestionVO>> list(
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "outlineId", required = false) Long outlineId,
            @RequestParam(name = "kpId", required = false) Long kpId,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return R.ok(wrongBookService.list(StpUtil.getLoginIdAsLong(), subjectId, status,
                outlineId, kpId, sortBy, PageQuery.of(pageNum, pageSize)));
    }

    /** 移出错题本（05 §5.7 第 27 条）：置 {@code REMOVED}，数据保留。 */
    @DeleteMapping("/{questionId}")
    @SaCheckLogin
    @Operation(summary = "移出错题本", description = "置 REMOVED（保留数据，可再恢复）。不在本子里返回 1003。")
    public R<Void> remove(@PathVariable("questionId") Long questionId) {
        wrongBookService.remove(StpUtil.getLoginIdAsLong(), questionId);
        return R.ok();
    }

    /** 恢复到错题本（05 §5.7 第 28 条）：置 {@code ACTIVE} 且连续答对清零。 */
    @PostMapping("/{questionId}/restore")
    @SaCheckLogin
    @Operation(summary = "恢复到错题本", description = "置 ACTIVE 并把连续答对清零。")
    public R<Void> restore(@PathVariable("questionId") Long questionId) {
        wrongBookService.restore(StpUtil.getLoginIdAsLong(), questionId);
        return R.ok();
    }
}