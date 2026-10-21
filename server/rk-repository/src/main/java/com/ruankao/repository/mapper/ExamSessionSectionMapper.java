package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.ExamSessionSection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话分节（{@code exam_session_section}，02 §2.5.6、06 §6.9.1）。 */
@Mapper
public interface ExamSessionSectionMapper {

    int insertBatch(@Param("rows") List<ExamSessionSection> rows);

    List<ExamSessionSection> selectBySession(@Param("sessionId") Long sessionId);

    ExamSessionSection selectOne(@Param("sessionId") Long sessionId,
                                 @Param("sectionNo") Integer sectionNo);

    /** 进入本节：置 IN_PROGRESS 并写 started_at（仅对 PENDING 生效）。 */
    int start(@Param("id") Long id, @Param("startedAt") LocalDateTime startedAt);

    /** 离开本节：置状态、写 used_sec 与 finished_at。 */
    int finish(@Param("id") Long id,
               @Param("status") String status,
               @Param("usedSec") Integer usedSec,
               @Param("finishedAt") LocalDateTime finishedAt);

    /** 随场次一起结束（超时/提前结束/交卷），只动仍未结束的分节。 */
    int finishSitting(@Param("sessionId") Long sessionId,
                      @Param("sittingNo") Integer sittingNo,
                      @Param("finishedAt") LocalDateTime finishedAt);

    int updateScore(@Param("id") Long id, @Param("objectiveScore") BigDecimal objectiveScore);

    /** 已用时长的实时校准入库（保存作答/心跳时调用）。 */
    int updateUsedSec(@Param("id") Long id, @Param("usedSec") Integer usedSec);
}
