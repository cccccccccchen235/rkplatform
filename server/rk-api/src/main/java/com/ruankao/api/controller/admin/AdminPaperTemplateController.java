package com.ruankao.api.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.api.support.ClientIpResolver;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.PaperTemplateSaveRequest;
import com.ruankao.domain.vo.PaperTemplateDetailVO;
import com.ruankao.domain.vo.PaperTemplateSaveResultVO;
import com.ruankao.service.question.PaperTemplateAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 卷面模板配置（05 §5.10 第 51/52 条）。
 *
 * <p>类级 {@code @SaCheckRole("ADMIN")}：与 {@link AdminSubjectController} 同一理由——
 * 管理端整块要求管理员角色，逐个方法标注只会让"漏标一个"变成一个可能发生的事故。
 *
 * <p>只有读与整体覆盖式写两个方法，没有单独的场次/分节接口：模板是一个整体，
 * 分开写会出现"场次已删、分节还指着它"的中间态。
 */
@RestController
@RequestMapping("/api/v1/admin/paper-templates")
@Validated
@SaCheckRole("ADMIN")
@Tag(name = "管理端 · 卷面模板", description = "05 §5.10 第 51/52 条：卷面模板读取与整体覆盖式更新")
public class AdminPaperTemplateController {

    private final PaperTemplateAdminService paperTemplateAdminService;

    public AdminPaperTemplateController(PaperTemplateAdminService paperTemplateAdminService) {
        this.paperTemplateAdminService = paperTemplateAdminService;
    }

    @GetMapping("/{subjectId}")
    @Operation(summary = "读取科目卷面模板（含场次与分节）",
            description = "科目不存在返回 3001；尚未配置模板返回 4001。响应可直接作为 PUT 的请求体。")
    public R<PaperTemplateDetailVO> detail(@PathVariable("subjectId") Long subjectId) {
        return R.ok(paperTemplateAdminService.detail(subjectId));
    }

    /**
     * 整体覆盖式更新（05 §5.10 第 52 条）。
     *
     * <p>操作者 ID 由 {@link StpUtil} 从会话取、IP 由 {@link ClientIpResolver} 从请求头取，
     * 交给 Service 写审计——Controller 不碰审计表（AC-3）。
     *
     * <p>保存成功但题量不足时，响应 {@code data.insufficientQuestions} 带警告，
     * 不阻断保存。
     */
    @PutMapping("/{subjectId}")
    @Operation(summary = "整体覆盖式更新卷面模板",
            description = "服务端强制校验：序号连续、时长自洽、满分口径与分节分值一致；不符返回 1001。"
                    + "题量不足时在 data.insufficientQuestions 返回警告。")
    public R<PaperTemplateSaveResultVO> save(@PathVariable("subjectId") Long subjectId,
                                             @Valid @RequestBody PaperTemplateSaveRequest request,
                                             HttpServletRequest httpRequest) {
        return R.ok(paperTemplateAdminService.save(
                subjectId, request, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest)));
    }
}
