package com.ruankao.api.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.api.support.ClientIpResolver;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.SubjectSaveRequest;
import com.ruankao.domain.dto.SubjectStatusRequest;
import com.ruankao.domain.vo.AdminSubjectVO;
import com.ruankao.service.question.AdminSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 科目维护（05 §5.10 第 37~40 条）。
 *
 * <p>类级 {@code @SaCheckRole("ADMIN")}：管理端整块要求管理员角色，
 * 逐个方法标注只会让"漏标一个"变成一个可能发生的事故。
 * Sa-Token 的注解校验器支持类级注解，ADR-02 的测试也认类级
 * （{@code AuthAnnotationTest.classLevelAnnotationMustBeCovered} 专门守这一点）。
 *
 * <p>操作者 ID 一律由 {@link StpUtil} 从会话取（AC-3：访问用户数据必须携带 user_id，
 * 且不能来自请求参数）；IP 由 {@link ClientIpResolver} 从请求头取。
 * 两者都交给 Service 去写审计——Controller 不碰审计表。
 */
@RestController
@RequestMapping("/api/v1/admin/subjects")
@Validated
@SaCheckRole("ADMIN")
@Tag(name = "管理端 · 科目", description = "05 §5.10 第 37~40 条：科目增删改查与启停")
public class AdminSubjectController {

    private final AdminSubjectService adminSubjectService;

    public AdminSubjectController(AdminSubjectService adminSubjectService) {
        this.adminSubjectService = adminSubjectService;
    }

    @GetMapping
    @Operation(summary = "科目列表（含已停用）")
    public R<List<AdminSubjectVO>> list() {
        return R.ok(adminSubjectService.listForAdmin());
    }

    @PostMapping
    @Operation(summary = "新增科目",
            description = "新科目一律为 ENABLED。返回新科目 ID。代码重复返回 1001。")
    public R<String> create(@Valid @RequestBody SubjectSaveRequest request,
                            HttpServletRequest httpRequest) {
        return R.ok(adminSubjectService.create(
                request, StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改科目",
            description = "level 不可修改；排序号不由本接口改动。")
    public R<Void> update(@PathVariable("id") Long id,
                          @Valid @RequestBody SubjectSaveRequest request,
                          HttpServletRequest httpRequest) {
        adminSubjectService.update(id, request,
                StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }

    /**
     * 启用 / 停用（05 §5.10 第 40 条）。
     *
     * <p><b>停用 = 下架</b>，是 P2 引入的第一个 AC-2 高危操作：
     * Service 会写一条 {@code sys_operation_log}（{@code module = SUBJECT}，
     * {@code action = DISABLE}/{@code ENABLE}）。状态没有变化时不做任何写入。
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "启用 / 停用科目（停用即下架）",
            description = "停用后考生端列表与详情随即不可见（缓存同时清除）。写 sys_operation_log。")
    public R<Void> changeStatus(@PathVariable("id") Long id,
                                @Valid @RequestBody SubjectStatusRequest request,
                                HttpServletRequest httpRequest) {
        adminSubjectService.changeStatus(id, request.status(),
                StpUtil.getLoginIdAsLong(), ClientIpResolver.resolve(httpRequest));
        return R.ok();
    }
}
