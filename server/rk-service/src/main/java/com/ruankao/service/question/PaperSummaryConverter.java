package com.ruankao.service.question;

import com.ruankao.domain.entity.PaperSection;
import com.ruankao.domain.entity.PaperSitting;
import com.ruankao.domain.entity.PaperTemplate;
import com.ruankao.domain.vo.PaperSummaryVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 卷面模板三张表 → {@code PaperSummaryVO}（05 §5.4 第 9 条）。
 *
 * <p>只下发作答相关字段：{@code perQuestionScore}、{@code durationMin/Max}、
 * {@code allowEarlySubmit} 一律<b>留在服务端</b>（{@link PaperSummaryVO.Section} 的说明）。
 *
 * <p>模板级 {@code totalDuration} 是<b>各场次之和</b>，在这里加出来，
 * 而不是从哪个列里读——{@code exam_paper_template} 上根本没有这一列
 * （已随 A5 裁决从 DDL 删除，总时长下沉到各场次）。
 */
public final class PaperSummaryConverter {

    private PaperSummaryConverter() {
    }

    public static PaperSummaryVO toVO(PaperTemplate template,
                                      List<PaperSitting> sittings,
                                      List<PaperSection> sections) {
        // 分节按 sittingNo 归拢；用 LinkedHashMap 保持 section_no 的既有顺序
        Map<Integer, List<PaperSummaryVO.Section>> sectionsBySitting = new LinkedHashMap<>();
        for (PaperSection section : sections) {
            sectionsBySitting
                    .computeIfAbsent(section.getSittingNo(), key -> new ArrayList<>())
                    .add(new PaperSummaryVO.Section(
                            section.getSectionNo(),
                            section.getSectionName(),
                            section.getSectionType(),
                            section.getQuestionCount(),
                            section.getSupportStatus()));
        }

        List<PaperSummaryVO.Sitting> sittingVOs = new ArrayList<>(sittings.size());
        int totalDuration = 0;
        for (PaperSitting sitting : sittings) {
            totalDuration += sitting.getTotalDuration() == null ? 0 : sitting.getTotalDuration();
            sittingVOs.add(new PaperSummaryVO.Sitting(
                    sitting.getSittingNo(),
                    sitting.getSittingName(),
                    sitting.getTotalDuration(),
                    sitting.getTotalMaxScore(),
                    sitting.getAutoScoredMaxScore(),
                    sectionsBySitting.getOrDefault(sitting.getSittingNo(), List.of())));
        }

        return new PaperSummaryVO(
                totalDuration,
                template.getTotalMaxScore(),
                template.getAutoScoredMaxScore(),
                sittingVOs);
    }
}
