package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 考试科目（{@code rk_subject}）。
 *
 * <p>刻意<b>不带任何 MyBatis 注解</b>：06 §6.1 禁止 rk-domain 依赖 MyBatis，
 * 对应的映射写在 rk-repository 的 XML Mapper 里（显式列名 + {@code mapUnderscoreToCamelCase}）。
 *
 * <p>除 {@code id} 外全部列均为 NOT NULL，故包装类型只用来表达"实体可能未加载该列"
 * （当前查询总是取全列，用包装类型是为了将来做部分列查询时不静默变 0）。
 */
@Getter
@Setter
@NoArgsConstructor
public class Subject {

    private Long id;

    /** JUNIOR / MIDDLE / SENIOR，见 {@link com.ruankao.domain.enums.SubjectLevel}。 */
    private String level;

    /** 科目代码，如 SD / SAD / IPT。业务唯一键。 */
    private String code;

    private String name;

    /** 卷面类型 JUNIOR_MID / SENIOR，决定该科目适用哪套卷面蓝图。 */
    private String paperType;

    /** 开考周期 BOTH / FIRST_HALF / SECOND_HALF。 */
    private String examPeriod;

    /** FULL 完整章节树 / SKELETON 仅科目。 */
    private String outlineStatus;

    /** SAMPLE 样例 / BULK 200~300 题。 */
    private String contentStatus;

    /** 科目简介，自行撰写（不得摘抄大纲正文，GR 约束）。 */
    private String intro;

    /** 冗余统计：已发布题量。 */
    private Integer questionCount;

    /**
     * 卷面满分，取自 {@code exam_paper_template.total_max_score}。
     *
     * <p><b>这一列不属于 {@code rk_subject}</b>：它是列表查询关联出来的只读投影，
     * 插入/更新永远不写它。之所以挂在本实体上而不是另建一个投影类型，是因为
     * MyBatis 的自动映射走 setter，本类没有 {@code @TableName}/{@code BaseMapper}，
     * SQL 全部手写在 XML 里，多一个字段不会被任何自动 SQL 生成器看到；
     * 而 {@code argNameBasedConstructorAutoMapping} 未开启，record 投影映射不可靠。
     *
     * <p>值为 {@code null} 有确切含义：该科目没有启用中的卷面模板
     * （05 §5.4 的 {@code onlyConfigured=true} 筛的就是这一批）。前端据此判断
     * "能不能发起模拟考"，不需要再单独发一个布尔字段。
     *
     * <p>唯一键 {@code uk_subject(subject_id)} 保证一个科目至多一份模板，
     * 因此关联不会让科目行翻倍。
     */
    private Integer paperTotalScore;

    private Integer sortNo;

    /** ENABLED / DISABLED。 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
