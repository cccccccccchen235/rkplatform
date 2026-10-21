package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.StatUserOutline;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户章节统计写入（{@code stat_user_outline}，04 §4.6）。
 *
 * <p>掌握度重算的产物按 {@code (user_id, outline_id)} 整体覆盖（06 §6.10.2）。
 * 没有作答的章节不写行——"没做过"由读取端用先验 {0.5} 兜底，
 * 给每个用户 × 每个章节都插一行会让这张表膨胀到"用户数 × 章节数"，
 * 而其中绝大多数行只是同一个先验值。
 */
@Mapper
public interface StatUserOutlineMapper {

    /** 批量 upsert 某用户某科目的章节统计。{@code rows} 为空时调用方不调用本方法。 */
    int upsertBatch(@Param("rows") List<StatUserOutline> rows);
}
