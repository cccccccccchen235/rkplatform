package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.QuestionFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目反馈与版权投诉写入（{@code rk_question_feedback}，04 §4.4；接口 05 §5.9 第 36 条）。
 *
 * <p>类型取值见 {@link com.ruankao.domain.enums.FeedbackType}；其中 {@code COPYRIGHT}
 * 在管理端为最高优先级，处理动作是下架题目（02 §2.6.3）。本 Mapper 目前只承担
 * 考生侧的"提交一条反馈"：新增记录初值 {@code status = 'PENDING'}，处理流转
 * （PROCESSED / IGNORED）属于管理端后续能力，不在本轮范围内。
 *
 * <p>24 小时提交次数限流（05 §5.9：同一用户对同一题最多 3 次）由服务层用 Redis 完成，
 * 不落库、不在这里实现——限流是 TTL 语义，进库会引入"过期清理"的额外负担。
 *
 * <p>SQL 写在 {@code resources/mapper/QuestionFeedbackMapper.xml}。
 */
@Mapper
public interface QuestionFeedbackMapper {

    /**
     * 追加一条反馈；自增主键回填到 {@code feedback.id}。
     *
     * <p>{@code status} 初值由调用方按业务给定（当前恒为 {@code 'PENDING'}），
     * 不在 XML 里写死，避免"默认值"在代码与 DDL 两处各有一份。
     */
    int insert(QuestionFeedback feedback);
}