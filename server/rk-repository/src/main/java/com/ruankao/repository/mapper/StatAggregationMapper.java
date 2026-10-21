package com.ruankao.repository.mapper;

import com.ruankao.domain.vo.ActiveUserSubjectRow;
import com.ruankao.domain.vo.StatAnswerRow;
import com.ruankao.domain.vo.StatQuestionKpRow;
import com.ruankao.domain.vo.StatRefCountRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 统计重算的**读**端（06 §6.10.2）：从流水与题库里取重算所需的原始事实与分母。
 *
 * <p>与四张 {@code stat_*} 写入 mapper 分开：那些是"聚合缓存的读写"，
 * 这里是"缓存该从哪里派生"——混在一起会让"这张表到底是不是事实来源"变得含糊，
 * 而本项目的第一条数据纪律就是<b>事实只在流水表</b>（04 §4.3、06 §6.10）。
 *
 * <p>为什么不做 SQL 窗口函数去重（06 §6.10.2 示例用的是 {@code ROW_NUMBER()}）：
 * 去重口径有一条硬性验收——"一题先对后错，{@code accuracy} 必须为 0"。
 * SQL 里的窗口函数只有连库集成测试才能覆盖，而本项目的测试是纯单测（不连 MySQL）。
 * 把去重下沉到 {@code StatAggregator} 让这条口径有了真正的单测（见该类的测试）。
 * 语义与文档一致，只是"唯一实现"从 SQL 移到了 Java，属实现层选择而非契约偏离。
 */
@Mapper
public interface StatAggregationMapper {

    /**
     * 某用户某科目的全部客观题作答流水（重算的原始事实）。
     *
     * <p>{@code is_correct IS NOT NULL} 排除主观题：它们不判分，混进来会把
     * {@code exposure} 的分子做大而正确率算错（04 §4.3）。
     * 返回<b>不去重</b>的全量行，去重由 {@code StatAggregator} 完成。
     *
     * @param limit 硬上界（AC-4）；单用户单科目流水在数千量级，20000 远超实际
     */
    List<StatAnswerRow> selectAnswerRows(@Param("userId") Long userId,
                                         @Param("subjectId") Long subjectId,
                                         @Param("limit") int limit);

    /**
     * 某用户某科目下已作答题目 → 知识点关联（去重后的题目集合）。
     *
     * <p>不过滤知识点的 {@code status}：统计按题库事实聚合，停用标签也是该题的考点
     * （见 {@code StatQuestionKpRow} 的说明）。
     */
    List<StatQuestionKpRow> selectQuestionKpLinks(@Param("userId") Long userId,
                                                  @Param("subjectId") Long subjectId,
                                                  @Param("limit") int limit);

    /** 某科目各叶子章节的已发布题量（章节掌握度的分母 {@code n}）。 */
    List<StatRefCountRow> selectOutlinePublishedCounts(@Param("subjectId") Long subjectId);

    /** 某科目各知识点的已发布题量（知识点掌握度的分母 {@code n}）。 */
    List<StatRefCountRow> selectKpPublishedCounts(@Param("subjectId") Long subjectId);

    /**
     * 近 {@code days} 天有作答的用户 × 其全部作答科目（夜间全量重算的待办清单）。
     *
     * <p>"活跃"按用户判定、科目取全部做过题的：只回填最近做过的那一门会让
     * 用户早先科目的掌握度因时间衰减而长期停滞（06 §6.13）。
     *
     * @param limit 硬上界（AC-4）
     */
    List<ActiveUserSubjectRow> selectActiveUserSubjects(@Param("days") int days,
                                                        @Param("limit") int limit);
}
