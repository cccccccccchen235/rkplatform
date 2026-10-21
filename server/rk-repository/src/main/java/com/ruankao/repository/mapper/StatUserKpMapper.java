package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.StatUserKp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户知识点统计写入（{@code stat_user_kp}，04 §4.6）。
 *
 * <p>口径与 {@code stat_user_outline} 相同，只是聚合维度换成 {@code kp_id}。
 * 它是 {@code onlyUnmastered} 过滤器的数据来源（05 §5.5 第 12 条）。
 */
@Mapper
public interface StatUserKpMapper {

    /** 批量 upsert 某用户某科目的知识点统计。{@code rows} 为空时调用方不调用本方法。 */
    int upsertBatch(@Param("rows") List<StatUserKp> rows);
}
