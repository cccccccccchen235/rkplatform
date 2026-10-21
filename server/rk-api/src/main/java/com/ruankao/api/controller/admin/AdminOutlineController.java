package com.ruankao.api.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.api.support.ClientIpResolver;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.OutlineNodeRequest;
import com.ruankao.domain.dto.OutlineSortItem;
import com.ruankao.domain.vo.OutlineNodeVO;
import com.ruankao.service.question.OutlineService;
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
 * 管理端 · 章节树维护（05 §5.10 第 41~45 条）。
 *
 * <p>类级 {@code @SaCheckRole("ADMIN")}，理由同 {@code AdminSubjectController}。
 *
 * <p>读接口带 {@code includeDisabled = true}：管理端要能看见并重新启用已停用章节，
 * 也要能看见编排中被停用的部分。
 */
@RestController
@RequestMapping("/api/v1/admin/outline")
@Validated
@SaCheckRole("ADMIN")
@Tag(name = "管理端 · 章节树", description = "05 §5.10 第 41~45 条：章节树读写与排序")
public class AdminOutlineController {

    private final OutlineService outlineService;

    public AdminOutlineController(OutlineService outlineService) {
        this.outlineService = outlineService;
    }

    /** 章节树（含已停用）。{@code subjectId} 必填——章节树总是属于某个科目，没有"全部"这种读法。 */
    @GetMapping
    @Operation(summary = "章节树（含已停用）", description = "返回根节点数组，根节点 parentId 恒为 \"0\"。")
    public R<List<OutlineNodeVO>> tree(@RequestParam("subjectId") Long subjectId) {
        return R.ok(outlineService.getTree(subjectId, true));
    }

    @PostMapping
    @Operation(summary = "新增章节",
            description = "层级由 parentId 推导（根为 \"0\"），深于 3 层返回 1001。返回新章节 ID。")
    public R<String> create(@Valid @RequestBody OutlineNodeRequest request,
                            HttpServletRequest httpRequest) {
        return R.ok(outlineService.createNode(
                request, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改章节",
            description = "可改名、改父（层级级联平移子孙）、改排序号。成环或超过 3 层返回 1001。")
    public R<Void> update(@PathVariable("id") Long id,
                          @Valid @RequestBody OutlineNodeRequest request,
                          HttpServletRequest httpRequest) {
        outlineService.updateNode(id, request,
                StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }

    /**
     * 删除章节（05 §5.10 第 44 条）。
     *
     * <p>有题目引用返回 {@code 1005}；<b>仍有子章节时同样返回 1005</b>
     * （05 只写了前一条，后一条见 Service 的说明）。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除章节",
            description = "有题目引用或仍有子章节时返回 1005，不做级联删除。")
    public R<Void> delete(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        outlineService.deleteNode(id, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }

    /**
     * 批量排序（05 §5.10 第 45 条）。
     *
     * <p>请求体为<b>裸数组</b> {@code [{id, parentId, sortNo}]}，是"整棵树的新顺序"的全量提交，
     * 不是增量：未提到的节点保持原位。跨层拖拽（改 {@code parentId}）也在同一次提交里表达。
     */
    @PutMapping("/sort")
    @Operation(summary = "批量排序（全量提交）",
            description = "请求体为 [{id, parentId, sortNo}]。一次只能提交同一科目的章节。")
    public R<Void> sort(@Valid @RequestBody List<OutlineSortItem> items,
                        HttpServletRequest httpRequest) {
        outlineService.sort(items, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }
}
