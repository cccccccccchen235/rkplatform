package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.ExamSessionSitting;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话场次（{@code exam_session_sitting}，06 §6.9）。 */
@Mapper
public interface ExamSessionSittingMapper {

    int insertBatch(@Param("rows") List<ExamSessionSitting> rows);

    List<ExamSessionSitting> selectBySession(@Param("sessionId") Long sessionId);

    ExamSessionSitting selectOne(@Param("sessionId") Long sessionId,
                                 @Param("sittingNo") Integer sittingNo);

    /** PENDING → IN_PROGRESS，写入 started_at。 */
    int start(@Param("id") Long id,
              @Param("startedAt") LocalDateTime startedAt,
              @Param("remainingSec") Integer remainingSec);

    /** 结束：剩余时长冻结为 0，落场次得分。 */
    int finish(@Param("id") Long id,
               @Param("objectiveScore") BigDecimal objectiveScore,
               @Param("finishedAt") LocalDateTime finishedAt);

    /** 同步剩余时长镜像与当前分节。 */
    int updateProgress(@Param("id") Long id,
                       @Param("remainingSec") Integer remainingSec,
                       @Param("currentSectionNo") Integer currentSectionNo,
                       @Param("objectiveScore") BigDecimal objectiveScore);

    /** 超时兜底扫描（06 §6.9.2）：IN_PROGRESS 且已超过 total_duration_sec + 60s。 */
    List<ExamSessionSitting> selectExpired(@Param("limit") int limit);

    /** 会话级滞留（评审 G1）：会话 IN_PROGRESS、当前场次 PENDING、上一场次结束超 N 小时。 */
    List<ExamSessionSitting> selectStalePending(@Param("hours") int hours,
                                                @Param("limit") int limit);
}
