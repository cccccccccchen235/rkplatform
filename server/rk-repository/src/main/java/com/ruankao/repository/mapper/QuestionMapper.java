package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.Question;
import com.ruankao.domain.entity.QuestionOption;
import com.ruankao.domain.vo.QuestionKpRowVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 题目查询：练习组卷的候选池 + 按 ID 回读（06 §6.6.3、§6.6.5）。
 *
 * <p>SQL 一律写在同名 XML 中（实体在 rk-domain 且不带 MyBatis 注解）。
 *
 * <p><b>入卷的唯一条件是 {@code status = 'PUBLISHED'}（AC-5）。</b>这一条写在 XML 的
 * {@code publishedPredicate} 片段里，六种练习模式的取题 SQL 共用一个片段——若在每个 mode
 * 的 SQL 里各写一遍，就会出现"某个模式忘了加 PUBLISHED"这种只在那一个模式下泄漏草稿题的缺陷。
 *
 * <p><b>随机取数只落 {@code ORDER BY RAND()} 一条分支</b>（06 §6.6.5 的规模阈值：
 * 候选池 &lt; 2000 时该排序开销可忽略）。本项目单章节池远小于 2000，因此
 * "随机偏移多轮取样"那条分支与配套的 {@code PickStrategy} 接口<b>暂不落地</b>，
 * 待题量增长时再按 06 §6.6.5 补上（已登记为对 06 文档的偏离）。
 *
 * <p>本接口<b>不</b>做"防重复窗口"过滤（06 §6.6.4）：那是模拟考组卷要避开的，
 * 练习若也避开，错题重做与收藏练习会立刻取不到题——被排除的正是用户想再练的那些。
 */
@Mapper
public interface QuestionMapper {

    /**
     * {@code SAMPLE}：全科目随机，供游客（05 §5.5 第 12 条，{@code count ≤ 5}）。
     *
     * @param userId 仅 {@code onlyUnmastered=true} 时使用；游客传 {@code null}
     * @param difficulty 难度过滤，{@code null} = 不过滤（05 §5.5 第 12 条的 {@code difficulty}）
     */
    List<Question> selectRandomBySubject(@Param("subjectId") Long subjectId,
                                         @Param("userId") Long userId,
                                         @Param("onlyUnmastered") boolean onlyUnmastered,
                                         @Param("difficulty") Integer difficulty,
                                         @Param("limit") int limit);

    /**
     * {@code CHAPTER}：按章节（含子孙）。
     *
     * <p>{@code outlineIds} 由服务层从大纲树缓存里展开"该节点及其全部子孙叶子"后传入——
     * 不在 SQL 里写递归 CTE：{@code rk_question.outline_id} 只挂叶子章节，
     * 而树本身已经有 1 小时缓存（{@code rk:cache:outline:{subjectId}}），
     * 在 Java 里走一遍树比每次组卷都跑一遍递归查询便宜。
     *
     * <p>调用方必须保证 {@code outlineIds} 非空：空集合会生成 {@code IN ()} 语法错，
     * 由 XML 里的 {@code 1 = 0} 短路兜住（返回空池，而不是抛异常）。
     */
    List<Question> selectRandomByOutlines(@Param("subjectId") Long subjectId,
                                          @Param("outlineIds") List<Long> outlineIds,
                                          @Param("userId") Long userId,
                                          @Param("onlyUnmastered") boolean onlyUnmastered,
                                          @Param("difficulty") Integer difficulty,
                                          @Param("limit") int limit);

    /** {@code KP}：按知识点（{@code JOIN rk_question_kp}）。 */
    List<Question> selectRandomByKp(@Param("subjectId") Long subjectId,
                                    @Param("kpId") Long kpId,
                                    @Param("userId") Long userId,
                                    @Param("onlyUnmastered") boolean onlyUnmastered,
                                    @Param("difficulty") Integer difficulty,
                                    @Param("limit") int limit);

    /**
     * {@code SEQUENCE}：按 {@code id ASC} 顺序取 {@code id > fromSequence} 的题，**不随机**。
     *
     * <p>刻意不接受 {@code onlyUnmastered} / {@code difficulty}：顺序练习是"从游标往下走"，
     * 一旦按掌握度或难度过滤，题号会出现空洞，用户看到的是"跳到下一题却跳过了好几号"，
     * 而"继续上次练习"的游标语义（{@code last_sequence}）也随之失真。
     */
    List<Question> selectBySequence(@Param("subjectId") Long subjectId,
                                    @Param("fromSequence") Long fromSequence,
                                    @Param("limit") int limit);

    /** {@code FAVORITE}：收藏的题里随机取（{@code JOIN user_favorite}）。 */
    List<Question> selectRandomByFavorite(@Param("userId") Long userId,
                                          @Param("subjectId") Long subjectId,
                                          @Param("onlyUnmastered") boolean onlyUnmastered,
                                          @Param("difficulty") Integer difficulty,
                                          @Param("limit") int limit);

    /**
     * {@code WRONG_REDO}：错题本里 {@code status='ACTIVE'} 的题，**错得多的优先**
     * （06 §6.6.3），同错次数的再随机，避免每次都从同一道开始。
     */
    List<Question> selectRandomByWrong(@Param("userId") Long userId,
                                       @Param("subjectId") Long subjectId,
                                       @Param("onlyUnmastered") boolean onlyUnmastered,
                                       @Param("difficulty") Integer difficulty,
                                       @Param("limit") int limit);

    /**
     * 候选池题量（05 §5.5 第 14 条 {@code totalCount} 的来源）。
     *
     * <p>与取题方法**共用同一批 SQL 片段**，这样"进度条的分母"与"实际能取到多少题"
     * 不会各算各的——两者口径一旦分叉，表现是"进度条显示 30 题、实际只出 12 题"。
     *
     * @param mode             {@code CHAPTER/KP/SEQUENCE/FAVORITE/WRONG_REDO/SAMPLE}
     * @param refId            {@code CHAPTER} / {@code KP} 的章节或知识点 ID，其余为 {@code null}
     * @param outlineIds       {@code CHAPTER} 的展开后章节集合
     * @param onlyUnmastered   与取题保持一致，传 {@code false} 时该过滤器不生效
     */
    long countPracticePool(@Param("subjectId") Long subjectId,
                           @Param("mode") String mode,
                           @Param("refId") Long refId,
                           @Param("outlineIds") List<Long> outlineIds,
                           @Param("userId") Long userId,
                           @Param("onlyUnmastered") boolean onlyUnmastered,
                           @Param("difficulty") Integer difficulty);

    /**
     * 按 ID 批量回读完整题目。{@code ids} 为空时返回空列表。
     *
     * <p><b>答题上报必须走这里拿题、再判分</b>，不能信客户端传来的题干或答案
     * （AC-3：客户端上报的对错不可信）。
     */
    List<Question> selectByIds(@Param("ids") List<Long> ids);

    /** 按题目 ID 批量取选项（仅客观题有）。按 {@code question_id, sort_no} 升序。 */
    List<QuestionOption> selectOptionsByQuestionIds(@Param("ids") List<Long> ids);

    /**
     * 按题目 ID 批量取知识点关联（题目 → 标签），用于装配 {@code QuestionVO.knowledgePoints}
     * （05 §5.2：{@code [{id, name}]}）。
     *
     * <p>只返回 {@code ENABLED} 的标签：考生端看不到已停用的标签
     * （05 §5.2 的 {@code KnowledgePointVO} 说明）。{@code ids} 为空时返回空列表。
     */
    List<QuestionKpRowVO> selectKnowledgePointsByQuestionIds(@Param("ids") List<Long> ids);
}