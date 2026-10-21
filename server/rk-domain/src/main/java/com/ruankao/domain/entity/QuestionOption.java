package com.ruankao.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 题目选项（{@code rk_question_option}），<b>仅客观题有值</b>（04 §4.4）。
 *
 * <p>{@code uk_question_key(question_id, option_key)} 保证同一题内选项 key 不重复；
 * 主观题/论文不写本表，参考答案与评分要点落在 {@link Question}。
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionOption {

    private Long id;

    private Long questionId;

    /** 选项 key，A~F 大写字母（CHAR(1)）。 */
    private String optionKey;

    /** 选项内容，Markdown + KaTeX。 */
    private String content;

    private Integer sortNo;
}