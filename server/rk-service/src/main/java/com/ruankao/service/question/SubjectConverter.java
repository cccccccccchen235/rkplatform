package com.ruankao.service.question;

import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.PaperSummaryVO;
import com.ruankao.domain.vo.SubjectVO;

/**
 * 科目实体 → VO（03 §3.3.2：每个业务包内配一个 Converter）。
 *
 * <p>手写，不引 MapStruct：转换逻辑只有字段搬运，手写更透明且零构建期成本。
 *
 * <p>必须是 {@code public}：实现类在 {@code question.impl} 子包内，包私有访问不到。
 */
public final class SubjectConverter {

    private SubjectConverter() {
    }

    /**
     * @param paperSummary 卷面摘要；列表接口传 null（05 §5.4 把 paperSummary 列为详情接口的特性）
     */
    public static SubjectVO toVO(Subject entity, PaperSummaryVO paperSummary) {
        return new SubjectVO(
                // 主键是 BIGINT UNSIGNED，转字符串下发以避免前端 JS 丢精度
                String.valueOf(entity.getId()),
                entity.getLevel(),
                entity.getCode(),
                entity.getName(),
                entity.getPaperType(),
                entity.getExamPeriod(),
                entity.getOutlineStatus(),
                entity.getContentStatus(),
                entity.getQuestionCount(),
                // 关联投影列：列表查询关联 exam_paper_template 得到，null = 无启用中的模板
                entity.getPaperTotalScore(),
                entity.getIntro(),
                paperSummary);
    }
}
