package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.R;
import com.ruankao.domain.vo.StatOutlineNodeVO;
import com.ruankao.domain.vo.StatOverviewVO;
import com.ruankao.domain.vo.StatTrendPointVO;
import com.ruankao.domain.vo.WeakKpVO;
import com.ruankao.service.stat.StatisticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习统计（05 §5.8 第 32~35 条）。
 *
 * <p>四个接口都是 {@code USER} 权限，因此 {@code @SaCheckLogin} 标在类上即可——
 * 这里没有"某一条允许游客"的反例，不需要逐方法标注（与 {@code PracticeController} 相反）。
 *
 * <p>{@code subjectId} 全部可选：缺省取用户的 {@code currentSubjectId}。前端第一次进入
 * 统计页时通常会显式传当前科目，缺省值主要服务"用户还没在前端选过科目"的场景。
 */
@RestController
@RequestMapping("/api/v1/stats")
@SaCheckLogin
@Validated
@Tag(name = "学习统计", description = "05 §5.8：总览、趋势、章节掌握度、薄弱知识点")
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;

    public StatisticsController(StatisticsQueryService statisticsQueryService) {
        this.statisticsQueryService = statisticsQueryService;
    }

    /** 总览（05 §5.8 第 32 条）。未做过任何题的科目返回零值与先验掌握度，不报错。 */
    @GetMapping("/overview")
    @Operation(summary = "统计总览",
            description = "科目维度的做题量/正确率/掌握度 + 用户维度的学习天数/连续天数/今日数据。")
    public R<StatOverviewVO> overview(@RequestParam(name = "subjectId", required = false) Long subjectId) {
        return R.ok(statisticsQueryService.overview(StpUtil.getLoginIdAsLong(), subjectId));
    }

    /** 每日趋势（05 §5.8 第 33 条）。{@code days} 默认 30、上限 90；缺失日期补 0。 */
    @GetMapping("/trend")
    @Operation(summary = "每日趋势", description = "缺失日期由服务端补 0；days 默认 30、上限 90。")
    public R<List<StatTrendPointVO>> trend(
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "days", required = false) Integer days) {
        return R.ok(statisticsQueryService.trend(StpUtil.getLoginIdAsLong(), subjectId, days));
    }

    /** 章节掌握度树（05 §5.8 第 34 条）。父节点为其子节点按题量加权。 */
    @GetMapping("/outline")
    @Operation(summary = "章节掌握度", description = "树形返回；父节点掌握度为其子节点按已发布题量加权。")
    public R<List<StatOutlineNodeVO>> outline(
            @RequestParam(name = "subjectId", required = false) Long subjectId) {
        return R.ok(statisticsQueryService.outline(StpUtil.getLoginIdAsLong(), subjectId));
    }

    /** 薄弱知识点（05 §5.8 第 35 条）。按掌握度升序，{@code limit} 默认 10、上限 30。 */
    @GetMapping("/weak-kp")
    @Operation(summary = "薄弱知识点", description = "掌握度升序；limit 默认 10、上限 30；maxMastery 默认 0.6。")
    public R<List<WeakKpVO>> weakKp(
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "maxMastery", required = false) Double maxMastery) {
        return R.ok(statisticsQueryService.weakKp(
                StpUtil.getLoginIdAsLong(), subjectId, limit, maxMastery));
    }
}
