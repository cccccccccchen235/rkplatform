package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识点标签（{@code rk_knowledge_point}）。
 *
 * <p>与 {@link Subject} 同规矩：不带 MyBatis 注解，映射在 XML 里。
 *
 * <p>{@code aliases} 在库里是**逗号分隔的单列**（04 §4.5），不是关联表。合并检索靠
 * {@code LIKE}，题目与知识点的多对多在 {@code rk_question_knowledge} 上——本实体
 * 只承载标签自身的元数据，不承载关联。
 */
@Getter
@Setter
@NoArgsConstructor
public class KnowledgePoint {

    private Long id;

    private Long subjectId;

    /** 标签名；{@code uk_subject_name(subject_id, name)} 保证同科目内唯一。 */
    private String name;

    /** 同义词，逗号分隔；可为 null。 */
    private String aliases;

    /** 冗余统计：本标签下的已发布题量。 */
    private Integer questionCount;

    /** ENABLED / DISABLED。 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
