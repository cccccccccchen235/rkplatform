package com.ruankao.domain.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 题目 → 知识点的关联投影（{@code rk_question_kp} JOIN {@code rk_knowledge_point}）。
 *
 * <p>它<b>不是</b>接口契约里的 {@code KnowledgePointVO}：那个 {@code record} 描述的是
 * "一个知识点标签"（05 §5.2），而这里描述的是"某道题挂了哪些标签"，
 * 多一个 {@code questionId} 用于在内存里把同一批题的标签分回各自的题。
 *
 * <p>用可变类而不是 record：MyBatis 的自动映射走 setter，本项目未开启
 * {@code argNameBasedConstructorAutoMapping}（见 {@code Subject} 的说明），
 * record 投影映射不可靠。
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionKpRowVO {

    /** 题目 ID，用于把结果分组回各自的题。 */
    private Long questionId;

    /** 知识点 ID（05 §5.2 的 {@code knowledgePoints[].id}）。 */
    private Long kpId;

    /** 知识点名（同科目内唯一）。 */
    private String kpName;
}