package com.ruankao.service.stat;

import com.ruankao.domain.vo.StatOutlineNodeVO;
import com.ruankao.domain.vo.StatOverviewVO;
import com.ruankao.domain.vo.StatTrendPointVO;
import com.ruankao.domain.vo.WeakKpVO;
import java.util.List;

/**
 * 学习统计读取（05 §5.8 第 32~35 条）。
 *
 * <p>与 {@link StatisticsService#refreshIfStale} 的配合是固定的：每个读取入口<b>先</b>
 * 把可能的脏掌握度重算掉，再查——否则页面展示的是上一次读取时的画像。
 *
 * <p>{@code subjectId} 缺省时取用户的 {@code currentSubjectId}；两者都没有时抛
 * {@code 5001}（前端据此渲染"先去选科目/先做几道题"的空态，而不是一个报错弹窗）。
 */
public interface StatisticsQueryService {

    StatOverviewVO overview(Long userId, Long subjectId);

    /** @param days 缺省 30，上限 90；缺失日期由服务端补零 */
    List<StatTrendPointVO> trend(Long userId, Long subjectId, Integer days);

    List<StatOutlineNodeVO> outline(Long userId, Long subjectId);

    /** @param limit 缺省 10，上限 30；@param maxMastery 缺省 0.6 */
    List<WeakKpVO> weakKp(Long userId, Long subjectId, Integer limit, Double maxMastery);
}
