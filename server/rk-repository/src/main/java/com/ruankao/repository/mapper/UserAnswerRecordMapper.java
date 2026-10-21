package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.UserAnswerRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 答题记录读写（{@code user_answer_record}，04 §4.3；接口 05 §5.6）。
 *
 * <p>本表是本库<b>唯一的事实流水</b>，所有统计口径都由它派生，{@code stat_*} 表只是它的
 * 聚合缓存（04 §4.3、06 §6.10）。因此这里刻意只提供"追加一条"与"计数"两类操作：
 * 流水一旦写入就是不可变事实，不提供 update/delete，避免任何"回改历史"的入口。
 *
 * <p>注册由 {@link com.ruankao.repository.config.MapperScanConfig} 统一负责；
 * SQL 写在 {@code resources/mapper/UserAnswerRecordMapper.xml}——实体在 rk-domain，
 * 不得带 MyBatis 注解，映射靠显式列名 + {@code mapUnderscoreToCamelCase}。
 */
@Mapper
public interface UserAnswerRecordMapper {

    /** 追加一条作答流水；自增主键回填到 {@code record.id}。 */
    int insert(UserAnswerRecord record);

    /**
     * 统计某用户在某科目下的作答行数。
     *
     * <p>用于断言"落库行数 == 本次实际提交的答案数"（05 §5.6 批量提交接口）：
     * 批量写若在组装阶段静默丢了答案，只看响应成功是发现不了的，只有把计数与
     * 提交数对一遍才暴露。口径是本表的原始行数，不叠加任何 status 过滤。
     *
     * @return 行数；无记录返回 0（COUNT 不会返回 NULL）
     */
    int countByUserAndSubject(@Param("userId") Long userId,
                              @Param("subjectId") Long subjectId);

    /**
     * 防重复窗口的**回填**查询（06 §6.6.4）：窗口内做过的去重题 ID。
     *
     * <p>唯一调用方是 {@code RepeatWindowDao} 在 Redis Set 未命中时重建它。这个 Set 是
     * 纯性能优化，可丢可重建，所以这条查询不需要与 Set 强一致。
     */
    List<Long> selectRecentQuestionIds(@Param("userId") Long userId,
                                       @Param("subjectId") Long subjectId,
                                       @Param("since") LocalDateTime since,
                                       @Param("limit") int limit);
}
