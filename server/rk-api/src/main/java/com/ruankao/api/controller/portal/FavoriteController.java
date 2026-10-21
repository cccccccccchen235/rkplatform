package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.PageQuery;
import com.ruankao.common.api.PageResult;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.FavoriteRequest;
import com.ruankao.domain.vo.QuestionVO;
import com.ruankao.service.practice.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目收藏（05 §5.7 第 29~31 条）。
 *
 * <p>收藏与取消都是**幂等**的（见 {@link FavoriteService}）：前端对收藏做乐观更新，
 * 重复的 POST / DELETE 不报错。题目不存在才是错误（3002）。
 */
@RestController
@RequestMapping("/api/v1/favorites")
@Validated
@Tag(name = "收藏", description = "05 §5.7：收藏、取消收藏、收藏列表")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** 收藏（05 §5.7 第 29 条）。重复收藏不报错。 */
    @PostMapping
    @SaCheckLogin
    @Operation(summary = "收藏题目", description = "幂等；题目不存在返回 3002。")
    public R<Void> add(@Valid @RequestBody FavoriteRequest request) {
        favoriteService.add(StpUtil.getLoginIdAsLong(), Long.valueOf(request.getQuestionId().trim()));
        return R.ok();
    }

    /** 取消收藏（05 §5.7 第 30 条）。未收藏也不报错。 */
    @DeleteMapping("/{questionId}")
    @SaCheckLogin
    @Operation(summary = "取消收藏", description = "幂等；未收藏也返回成功。")
    public R<Void> remove(@PathVariable("questionId") Long questionId) {
        favoriteService.remove(StpUtil.getLoginIdAsLong(), questionId);
        return R.ok();
    }

    /** 收藏列表（05 §5.7 第 31 条）：按收藏时间倒序分页。 */
    @GetMapping
    @SaCheckLogin
    @Operation(summary = "收藏列表", description = "按收藏时间倒序；返回不含答案的 QuestionVO。")
    public R<PageResult<QuestionVO>> list(
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return R.ok(favoriteService.list(StpUtil.getLoginIdAsLong(), subjectId,
                PageQuery.of(pageNum, pageSize)));
    }
}