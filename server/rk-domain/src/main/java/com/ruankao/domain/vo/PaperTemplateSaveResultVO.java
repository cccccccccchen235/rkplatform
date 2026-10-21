package com.ruankao.domain.vo;

import java.util.List;

/**
 * 卷面模板保存结果（05 §5.10 第 52 条的 {@code insufficientQuestions} 警告字段）。
 *
 * <p>保存成功（{@code code = 0}）但存在"配置的单选题数 > 该科目已发布单选题量"的分节时，
 * 用本对象把警告带回来，<b>不阻断保存</b>：管理员先把卷面配好、题库随后补齐是正常流程，
 * 而组卷侧会按 06 §6.6.2 的 L4 规则缩减题量。
 */
public record PaperTemplateSaveResultVO(List<InsufficientQuestionVO> insufficientQuestions) {

    /**
     * 题量不足警告。
     *
     * @param requiredCount  模板里该分节配置的题数
     * @param availableCount 该科目当前已发布的单选题数
     */
    public record InsufficientQuestionVO(Integer sectionNo,
                                         String sectionName,
                                         Integer requiredCount,
                                         Integer availableCount) {
    }
}
