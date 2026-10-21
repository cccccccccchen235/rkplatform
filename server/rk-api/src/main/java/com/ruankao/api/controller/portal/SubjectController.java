package com.ruankao.api.controller.portal;

import com.ruankao.common.api.R;
import com.ruankao.domain.vo.KnowledgePointVO;
import com.ruankao.domain.vo.OutlineNodeVO;
import com.ruankao.domain.vo.SubjectVO;
import com.ruankao.service.question.KnowledgePointService;
import com.ruankao.service.question.OutlineService;
import com.ruankao.service.question.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 科目与大纲 · 公开接口（05 §5.4）。
 *
 * <p>本控制器整体在白名单内（{@link com.ruankao.api.config.AuthWhitelist}：
 * {@code GET /api/v1/subjects} 与 {@code GET /api/v1/subjects/**}），
 * 故不标注 {@code @SaCheckLogin}。这不是"漏标"——ADR-02 的单元测试会同时校验
 * "要么有注解，要么在白名单里"，二者必居其一。
 *
 * <p>放行的理由：科目列表是首页首屏数据，未登录也要能看到有哪些科目，
 * 否则用户无法判断这个站点值不值得注册。
 */
@RestController
@RequestMapping("/api/v1/subjects")
@Validated
@Tag(name = "科目与大纲（公开）", description = "05 §5.4：科目列表、科目详情、大纲树、知识点")
public class SubjectController {

    private final SubjectService subjectService;
    private final OutlineService outlineService;
    private final KnowledgePointService knowledgePointService;

    public SubjectController(SubjectService subjectService,
                            OutlineService outlineService,
                            KnowledgePointService knowledgePointService) {
        this.subjectService = subjectService;
        this.outlineService = outlineService;
        this.knowledgePointService = knowledgePointService;
    }

    /**
     * 科目列表，按级别分组（05 §5.4 第 8 条）。
     *
     * @param level          可选，过滤级别：JUNIOR / MIDDLE / SENIOR
     * @param onlyConfigured 可选，默认 false；true 时仅返回已配置卷面模板的科目
     */
    @GetMapping
    @Operation(summary = "科目列表（按级别分组）",
            description = "响应 data 恒含 JUNIOR / MIDDLE / SENIOR 三个 key，空组为空数组。"
                    + "列表不含 paperSummary，详情接口才返回卷面结构。")
    public R<Map<String, List<SubjectVO>>> listSubjects(
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "onlyConfigured", defaultValue = "false") boolean onlyConfigured) {
        return R.ok(subjectService.listGroupedByLevel(level, onlyConfigured));
    }

    /**
     * 科目详情（05 §5.4 第 9 条）——<b>唯一带 {@code paperSummary} 的接口</b>。
     *
     * <p>没有启用中的卷面模板时 {@code paperSummary} 与 {@code paperTotalScore} 同为
     * {@code null}，前端据此显示"题库建设中/卷面待配置"，而不是一个空白页。
     */
    @GetMapping("/{id}")
    @Operation(summary = "科目详情（含卷面结构摘要）",
            description = "科目不存在或已停用时返回 3001。paperSummary 为空表示尚未配置卷面模板。")
    public R<SubjectVO> getSubject(@PathVariable("id") Long id) {
        return R.ok(subjectService.getDetail(id));
    }

    /**
     * 大纲章节树（05 §5.4 第 10 条）。
     *
     * <p>返回的是<b>根节点数组</b>（不是一棵带虚拟根的树），根节点的 {@code parentId} 恒为
     * {@code "0"}。叶子节点的 {@code children} 为空数组而非 {@code null}。
     */
    @GetMapping("/{id}/outline")
    @Operation(summary = "大纲章节树",
            description = "返回根节点数组，根节点 parentId 恒为 \"0\"。科目不存在或已停用时返回 3001。")
    public R<List<OutlineNodeVO>> getOutline(@PathVariable("id") Long id) {
        // 考生端：不含已停用章节
        return R.ok(outlineService.getTree(id, false));
    }

    /** 知识点标签列表（05 §5.4 第 11 条）。考生端只看得到 {@code ENABLED} 的标签。 */
    @GetMapping("/{id}/knowledge-points")
    @Operation(summary = "知识点标签列表",
            description = "只返回启用中的标签。aliases 是逗号分隔的单串，不是数组。")
    public R<List<KnowledgePointVO>> listKnowledgePoints(@PathVariable("id") Long id) {
        return R.ok(knowledgePointService.list(id, false));
    }
}
