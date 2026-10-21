package com.ruankao.repository.mapper;

import com.ruankao.domain.vo.ExamQuestionCandidate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 模拟考组卷的候选题查询（06 §6.6.2）。
 *
 * <p>只做"取回候选集"这一件事；配额分配与 L1~L4 降级在 {@code ExamPaperAssembler} 里用
 * 纯 Java 完成（见 {@link ExamQuestionCandidate} 的说明）。
 */
@Mapper
public interface ExamQuestionMapper {

    /**
     * 某科目某题型的全部已发布题，带上章节与用户最近作答时间。
     *
     * <p>LEFT JOIN 而不是 JOIN：从未做过的题（{@code last_answered_at} 为 null）恰恰是
     * 组卷最想要的，用 JOIN 会把它们全滤掉。
     */
    List<ExamQuestionCandidate> selectCandidates(@Param("userId") Long userId,
                                                 @Param("subjectId") Long subjectId,
                                                 @Param("questionType") String questionType,
                                                 @Param("limit") int limit);

    /** 某科目某题型的已发布题量（L4 缩减与 4002 判定用）。 */
    int countPublished(@Param("subjectId") Long subjectId,
                       @Param("questionType") String questionType);
}
