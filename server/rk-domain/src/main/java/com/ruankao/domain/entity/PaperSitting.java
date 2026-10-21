package com.ruankao.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 卷面场次（{@code exam_paper_sitting}），模板 → <b>场次</b> → 分节 的中间层（02 §2.4.1）。
 *
 * <p>场次的存在是有物理含义的：高级卷面的两个场次之间隔着午休，
 * 各有独立倒计时、互不自动衔接。因此 {@code totalDuration} 是"本场次"的时长，
 * 不是"一场考试"的时长——{@code PaperSummaryVO.totalDuration} 才是各场次之和。
 */
@Getter
@Setter
@NoArgsConstructor
public class PaperSitting {

    private Long id;

    private Long templateId;

    /** 场次序号 1/2。 */
    private Integer sittingNo;

    private String sittingName;

    /** 本场次总时长（分钟）；场次内分节共享。 */
    private Integer totalDuration;

    /** 本场次满分（NOT_SUPPORTED 分节不计）。 */
    private Integer totalMaxScore;

    /** 本场次可自动判分满分（仅客观题节）。 */
    private Integer autoScoredMaxScore;

    private Integer sortNo;
}
