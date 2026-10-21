package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.UserPracticeProgress;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 练习进度读写（{@code user_practice_progress}，04 §4.3；接口 05 §5.5、02 §2.5.4），
 * 服务于顺序练习的"继续上次练习"。
 *
 * <p>{@code outline_id} 采用 {@code NOT NULL DEFAULT 0}，<b>{@code 0} 是"不按章节"的确切哨兵值</b>，
 * 不是"未知/空"：唯一索引 {@code uk_user_subject_type_ref(user_id, subject_id, practice_type, outline_id)}
 * 只有在列非 NULL 时才真正去重，而 MySQL 唯一索引允许多个 NULL——若用 NULL，
 * 同一用户同一练习类型会攒出多条进度记录（04 §4.3 已注明）。因此本类所有涉及
 * {@code outlineId} 的入参都必须传 {@code 0} 而不是 {@code null}。
 *
 * <p>写入是幂等 upsert（05 §5.1「幂等」约定、§5.5）：同一个
 * {@code (user, subject, type, outline)} 无论提交多少次，都只保留最新一条进度。
 *
 * <p>SQL 写在 {@code resources/mapper/UserPracticeProgressMapper.xml}。
 */
@Mapper
public interface UserPracticeProgressMapper {

    /**
     * 幂等写入进度：命中唯一键则覆盖四个游标/计数列，否则插入。
     *
     * <p>{@code progress.outlineId} 为 {@code 0} 表示不按章节（见类注释），不可传 {@code null}。
     */
    int upsert(UserPracticeProgress progress);

    /**
     * 按增量累加作答题数（05 §5.5 第 14 条的 {@code answeredCount} 来源）。
     *
     * <p>与 {@link #upsert} 的区别：那个是"当前值快照"（整列覆盖），这个是"累加"
     * （{@code answered_count = answered_count + 本次}）。答题上报是追加事件，
     * 用快照式写入会把累计数抹掉，所以两者必须是两个方法而不是一个带开关的方法——
     * 语义不同，参数也不同（这里没有 {@code lastSequence}）。
     *
     * <p>只在行已存在或需要新建时都能工作：新建时 {@code last_sequence = 0}、
     * {@code last_question_id = NULL}，交给 {@code PUT /practice/progress} 去推进游标。
     */
    int incrementCounts(@Param("userId") Long userId,
                        @Param("subjectId") Long subjectId,
                        @Param("practiceType") String practiceType,
                        @Param("outlineId") Long outlineId,
                        @Param("answeredDelta") int answeredDelta,
                        @Param("correctDelta") int correctDelta);

    /**
     * 取某 {@code (user, subject, type, outline)} 的唯一进度；不存在返回 {@code null}。
     *
     * @param outlineId <b>必须传 {@code 0} 表示不按章节</b>，不能传 {@code null}：
     *                  库中该列是 {@code NOT NULL}，达 {@code null} 会写成
     *                  {@code outline_id = NULL} 而永远匹配不到任何行（静默返回 null）
     */
    UserPracticeProgress selectOne(@Param("userId") Long userId,
                                   @Param("subjectId") Long subjectId,
                                   @Param("practiceType") String practiceType,
                                   @Param("outlineId") Long outlineId);

    /** 某用户在某科目下的全部进度记录（用于汇总"继续上次练习"入口）。 */
    List<UserPracticeProgress> selectBySubject(@Param("userId") Long userId,
                                               @Param("subjectId") Long subjectId);
}