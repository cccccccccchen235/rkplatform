package com.ruankao.service.question;

import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.AdminSubjectVO;
import java.util.List;

/**
 * 科目实体 → 管理端列表项（03 §3.3.2）。
 *
 * <p>与 {@link SubjectConverter} 分成两个类而不是加一个 {@code boolean forAdmin} 开关：
 * 那个开关会让两个字段集合在一条 {@code toVO} 里交织，读到一半必须回头看调用点
 * 才知道 {@code status} 这次到底有没有值。两个方法、两个返回类型，读起来是直白的。
 */
public final class AdminSubjectConverter {

    private AdminSubjectConverter() {
    }

    public static AdminSubjectVO toVO(Subject entity) {
        return new AdminSubjectVO(
                // BIGINT UNSIGNED → String，避免前端 JS 丢精度
                String.valueOf(entity.getId()),
                entity.getLevel(),
                entity.getCode(),
                entity.getName(),
                entity.getPaperType(),
                entity.getExamPeriod(),
                entity.getOutlineStatus(),
                entity.getContentStatus(),
                entity.getQuestionCount(),
                // 关联投影列：null = 没有启用中的卷面模板
                entity.getPaperTotalScore(),
                entity.getIntro(),
                entity.getSortNo(),
                entity.getStatus());
    }

    public static List<AdminSubjectVO> toVOList(List<Subject> entities) {
        return entities.stream().map(AdminSubjectConverter::toVO).toList();
    }
}
