package com.ruankao.api.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.api.support.ClientIpResolver;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.KnowledgePointMergeRequest;
import com.ruankao.domain.dto.KnowledgePointRequest;
import com.ruankao.domain.vo.KnowledgePointVO;
import com.ruankao.service.question.KnowledgePointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 知识点维护（05 §5.10 第 46~50 条）。
 *
 * <p>类级 {@code @SaCheckRole("ADMIN")}，理由同 {@code AdminSubjectController}。
 * 读接口带 {@code includeDisabled = true}（05 §5.10 第 46 条要求管理端见到停用项）。
 */
@RestController
@RequestMapping("/api/v1/admin/knowledge-points")
@Validated
@SaCheckRole("ADMIN")
@Tag(name = "管理端 · 知识点", description = "05 §5.10 第 46~50 条：知识点增删改查与合并")
public class AdminKnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    public AdminKnowledgePointController(KnowledgePointService knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    @GetMapping
    @Operation(summary = "知识点列表（含已停用）", description = "按题量降序。aliases 是逗号分隔的单串。")
    public R<List<KnowledgePointVO>> list(@RequestParam("subjectId") Long subjectId) {
        return R.ok(knowledgePointService.list(subjectId, true));
    }

    @PostMapping
    @Operation(summary = "新增知识点",
            description = "同科目内重名返回 1001。返回新知识点 ID。")
    public R<String> create(@Valid @RequestBody KnowledgePointRequest request,
                            HttpServletRequest httpRequest) {
        return R.ok(knowledgePointService.create(
                request, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改知识点（含 aliases）")
    public R<Void> update(@PathVariable("id") Long id,
                          @Valid @RequestBody KnowledgePointRequest request,
                          HttpServletRequest httpRequest) {
        knowledgePointService.update(id, request,
                StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }

    /** 删除知识点；仍被题目引用时返回 {@code 1005}。 */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识点", description = "仍被题目引用时返回 1005，不做级联删除。")
    public R<Void> delete(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        knowledgePointService.delete(id, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }

    /**
     * 合并标签（05 §5.10 第 50 条）。
     *
     * <p>一次事务内完成：迁移题目关联 → 把来源的名字与同义词并入目标的 {@code aliases}
     * → 删除来源标签 → 按关联表重算目标题量。来源中不得包含目标自身，也不得跨科目。
     */
    @PostMapping("/merge")
    @Operation(summary = "合并知识点",
            description = "请求体 {sourceIds:[], targetId}。迁移题目关联并删除来源标签。")
    public R<Void> merge(@Valid @RequestBody KnowledgePointMergeRequest request,
                         HttpServletRequest httpRequest) {
        knowledgePointService.merge(request, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }
}
