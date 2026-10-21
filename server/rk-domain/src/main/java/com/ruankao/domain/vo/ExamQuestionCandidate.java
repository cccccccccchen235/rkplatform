package com.ruankao.domain.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 组卷候选题目（模拟考，06 §6.6.2）。
 *
 * <p>一次取回某科目某题型的全部已发布题，配额分配、难度分档与 L1~L4 降级都在 Java 侧完成：
 * 算法分支很多（每章保底、最大余数、按档补足、跨章补足、缩减题量），写成 SQL 后每条分支
 * 都要单独验证；放在 Java 就能用纯单测把"配额怎么分"钉死。
 *
 * <p>{@code lastAnsweredAt} 供 L2 降级排序（最早做过的先复用）使用。
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamQuestionCandidate {

    private Long questionId;

    private Long outlineId;

    private Integer difficulty;

    /** 该用户最近一次做过这道题的时间；从未做过为 null。 */
    private LocalDateTime lastAnsweredAt;
}
