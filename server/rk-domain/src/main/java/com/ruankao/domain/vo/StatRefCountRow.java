package com.ruankao.domain.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统计重算的"分母"投影：某个聚合维度（章节 / 知识点）下的**已发布题量** {@code n}。
 *
 * <p>{@code refId} 的含义由查询决定——章节维度是 {@code outline_id}，
 * 知识点维度是 {@code kp_id}。两者结构相同，共用一个投影而不是各写一个字段完全一样的类。
 */
@Getter
@Setter
@NoArgsConstructor
public class StatRefCountRow {

    private Long refId;

    /** {@code status='PUBLISHED'} 的题量。 */
    private Integer questionCount;
}
