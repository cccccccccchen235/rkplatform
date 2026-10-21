package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.ExamSessionItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话题目快照（{@code exam_session_item}，02 §2.4.3）。 */
@Mapper
public interface ExamSessionItemMapper {

    int insertBatch(@Param("rows") List<ExamSessionItem> rows);

    List<ExamSessionItem> selectBySession(@Param("sessionId") Long sessionId);

    /** 取指定 itemId 的行，用于校验"这条 item 属于本会话"（4006）。 */
    List<ExamSessionItem> selectByIds(@Param("sessionId") Long sessionId,
                                      @Param("ids") List<Long> ids);

    /** 保存单条作答（不判分）。 */
    int updateAnswer(@Param("id") Long id,
                     @Param("sessionId") Long sessionId,
                     @Param("userAnswer") String userAnswer,
                     @Param("durationMs") Integer durationMs,
                     @Param("answeredAt") LocalDateTime answeredAt);

    /** 交卷判分：写对错、得分与主观题的参考答案/解析快照。 */
    int updateJudgement(@Param("id") Long id,
                        @Param("isCorrect") Boolean isCorrect,
                        @Param("score") BigDecimal score,
                        @Param("reference") String reference,
                        @Param("analysis") String analysis);
}
