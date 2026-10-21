package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 大纲章节节点（{@code rk_outline_node}）。
 *
 * <p>与 {@link Subject} 同规矩：<b>不带任何 MyBatis 注解</b>（06 §6.1 禁止 rk-domain 依赖
 * MyBatis），映射写在 rk-repository 的 XML 里。
 *
 * <p>{@code parentId} 用 {@code Long} 而非"可空"：DDL 里它是 {@code NOT NULL DEFAULT 0}，
 * <b>0 就是根</b>，不是 null。这一条是 04 §4.5 定的约定，也是 05 §5.2
 * {@code OutlineNodeVO.parentId} "根为 {@code "0"}" 的来源——两者必须一致，
 * 否则前端按 {@code parentId === "0"} 找根会一个都找不到。
 */
@Getter
@Setter
@NoArgsConstructor
public class OutlineNode {

    private Long id;

    private Long subjectId;

    /** 父节点 ID；<b>0 表示根</b>（不是 null）。 */
    private Long parentId;

    /** 1 / 2 / 3。深于 3 层由服务层拒绝（10 §10.4 的验收口径）。 */
    private Integer level;

    private String name;

    private Integer sortNo;

    /** OFFICIAL_TITLE 官方标题短语 / MANUAL 人工新增。GR 约束：只取标题短语，不抄大纲正文。 */
    private String origin;

    /** 冗余统计：本节点**含子节点**的已发布题量。 */
    private Integer questionCount;

    /** ENABLED / DISABLED。 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
