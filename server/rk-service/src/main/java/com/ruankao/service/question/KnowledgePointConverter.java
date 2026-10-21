package com.ruankao.service.question;

import com.ruankao.domain.entity.KnowledgePoint;
import com.ruankao.domain.vo.KnowledgePointVO;
import java.util.List;

/**
 * 知识点实体 → VO（03 §3.3.2）。
 */
public final class KnowledgePointConverter {

    private KnowledgePointConverter() {
    }

    public static KnowledgePointVO toVO(KnowledgePoint entity) {
        return new KnowledgePointVO(
                // BIGINT UNSIGNED → String，避免前端 JS 丢精度（与 SubjectVO 同规矩）
                String.valueOf(entity.getId()),
                String.valueOf(entity.getSubjectId()),
                entity.getName(),
                // 逗号分隔的单串，不在这里拆成数组：库与 05 §5.2 都是这个口径
                entity.getAliases(),
                entity.getQuestionCount(),
                entity.getStatus());
    }

    public static List<KnowledgePointVO> toVOList(List<KnowledgePoint> entities) {
        return entities.stream().map(KnowledgePointConverter::toVO).toList();
    }
}
