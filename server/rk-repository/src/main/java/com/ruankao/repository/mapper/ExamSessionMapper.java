package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.ExamSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 模拟考会话（{@code exam_session}，05 §5.6 第 16~18、23、25 条）。 */
@Mapper
public interface ExamSessionMapper {

    int insert(ExamSession session);

    /** 按主键 + 用户查（AC-3：用户数据访问必须带 user_id）。查不到返回 null。 */
    ExamSession selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /** 按主键查（仅定时任务用：它按会话 ID 反查 userId，不属于"用户请求"路径）。 */
    ExamSession selectById(@Param("id") Long id);

    /** 该用户是否有未结束（CREATED/IN_PROGRESS）的会话，用于 4003。 */
    ExamSession selectActiveByUser(@Param("userId") Long userId);

    /** 更新当前场次/分节与剩余时长镜像（镜像不参与判定，06 §6.9.1）。 */
    int updateProgress(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("currentSittingNo") Integer currentSittingNo,
                       @Param("currentSectionNo") Integer currentSectionNo,
                       @Param("remainingSec") Integer remainingSec,
                       @Param("startedAt") LocalDateTime startedAt);

    int markSubmitted(@Param("id") Long id, @Param("submittedAt") LocalDateTime submittedAt);

    int markScored(@Param("id") Long id,
                   @Param("objectiveScore") BigDecimal objectiveScore,
                   @Param("scoredAt") LocalDateTime scoredAt);

    int markAbandoned(@Param("id") Long id);

    List<ExamSession> selectHistory(@Param("userId") Long userId,
                                    @Param("subjectId") Long subjectId,
                                    @Param("includeAbandoned") boolean includeAbandoned,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    long countHistory(@Param("userId") Long userId,
                      @Param("subjectId") Long subjectId,
                      @Param("includeAbandoned") boolean includeAbandoned);
}
