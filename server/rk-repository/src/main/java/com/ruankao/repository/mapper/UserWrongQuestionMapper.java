package com.ruankao.repository.mapper;

import com.ruankao.domain.vo.WrongQuestionRowVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 错题本读写（{@code user_wrong_question}，04 §4.3；接口 05 §5.7 第 26~28 条）。
 *
 * <p>收录/移出规则见 04 §4.3 与 06 §6.8，本 Mapper 把它们拆成两个语义明确的写方法：
 * <ul>
 *   <li>{@link #upsertOnWrong}：答错，<b>可 INSERT 可 UPDATE</b>（首次答错才建行）；</li>
 *   <li>{@link #upsertOnCorrect}：答对，<b>只 UPDATE 不 INSERT</b>——从没答错过一道题时，
 *       一次答对不应凭空把它拉进错题本；同时永不触碰 {@code REMOVED} 行。</li>
 * </ul>
 *
 * <p>阈值（连续答对几次置 MASTERED）不写死：由调用方从 {@code sys_config} 的
 * {@code wrongbook.master.threshold} 读出后作为参数传入（06 §6.8）。
 *
 * <p>表上有 {@code uk_user_question(user_id, question_id)}，所有写操作都以
 * {@code (user_id, question_id)} 为定位条件，天然是单行操作。
 *
 * <p>SQL 写在 {@code resources/mapper/UserWrongQuestionMapper.xml}。
 */
@Mapper
public interface UserWrongQuestionMapper {

    /**
     * 答错时的收录/累加：无行则建立，有行则 {@code wrong_count + 1}、
     * {@code consecutive_correct} 归零、{@code status} 置回 {@code ACTIVE}。
     *
     * <p>置 {@code ACTIVE} 是无条件的（含原 {@code MASTERED}/{@code REMOVED} 的行）：
     * 又答错了就说明没掌握，应当重新出现在可用错题列表里（04 §4.3）。
     *
     * @param subjectId 冗余科目，避免错题列表回表 {@code rk_question}
     * @param now       本次作答时间，由调用方统一取值，保证同一次提交内各表时间一致
     */
    int upsertOnWrong(@Param("userId") Long userId,
                      @Param("questionId") Long questionId,
                      @Param("subjectId") Long subjectId,
                      @Param("answeredAt") LocalDateTime now);

    /**
     * 答对时的累加：仅更新已存在的行，{@code correct_count} 与
     * {@code consecutive_correct} 各 +1，新连续数达 {@code threshold} 置 {@code MASTERED}。
     *
     * <p><b>绝不 INSERT</b>，也<b>绝不更新 {@code status = 'REMOVED'} 的行</b>：
     * 用户手动移出错题本的意愿必须被尊重，不能因为他后来答对这一题而被悄悄改回来。
     *
     * @param subjectId 参数形状与 {@link #upsertOnWrong} 保持一致，便于调用方用同一组参数
     *                  分发"答错/答对"两条分支；纯 UPDATE 的定位只靠
     *                  {@code (user_id, question_id)} 唯一键，本参数不参与 SQL
     * @param threshold 连续答对阈值（{@code sys_config.wrongbook.master.threshold}）
     */
    int upsertOnCorrect(@Param("userId") Long userId,
                        @Param("questionId") Long questionId,
                        @Param("subjectId") Long subjectId,
                        @Param("answeredAt") LocalDateTime now,
                        @Param("threshold") int threshold);

    /** 手动移出错题本（05 §5.7 第 27 条）：置 {@code REMOVED}，保留数据。 */
    int markRemoved(@Param("userId") Long userId,
                    @Param("questionId") Long questionId);

    /**
     * 恢复到错题本（05 §5.7 第 28 条）：置 {@code ACTIVE} 并把 {@code consecutive_correct} 归零。
     *
     * <p>必须一并归零连续计数：恢复的用户意图是"这题重新算"，若保留旧的连续数，
     * 一次答对就可能立刻又跳回 {@code MASTERED}，恢复动作形同虚设。
     */
    int restore(@Param("userId") Long userId,
                @Param("questionId") Long questionId);

    /**
     * 错题本分页列表（05 §5.7 第 26 条）。
     *
     * @param status    ACTIVE / MASTERED / REMOVED；<b>null 或空串按 ACTIVE 处理</b>（契约默认值）
     * @param outlineId 叶子章节过滤，null 不过滤
     * @param kpId      知识点过滤，null 不过滤（非 null 时 JOIN {@code rk_question_kp}）
     * @param sortBy    {@code wrongCount} 或 {@code lastAnsweredAt}；其余/null 走默认 {@code lastAnsweredAt}
     * @param offset    起始偏移
     * @param limit     单页条数，由调用方按接口分页上限收敛后传入（AC-4 上界）
     */
    List<WrongQuestionRowVO> selectPage(@Param("userId") Long userId,
                                        @Param("subjectId") Long subjectId,
                                        @Param("status") String status,
                                        @Param("outlineId") Long outlineId,
                                        @Param("kpId") Long kpId,
                                        @Param("sortBy") String sortBy,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /** 与 {@link #selectPage} 同一过滤条件下的总数，供分页元信息使用。 */
    long countPage(@Param("userId") Long userId,
                   @Param("subjectId") Long subjectId,
                   @Param("status") String status,
                   @Param("outlineId") Long outlineId,
                   @Param("kpId") Long kpId);
}