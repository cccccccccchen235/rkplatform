package com.ruankao.domain.vo;

import java.util.List;

/** 离开分节的响应（05 §5.6 第 21 条）。 */
public record ExamSectionAdvanceVO(
        Integer currentSectionNo,
        Integer remainingSec,
        List<ExamSectionVO> sections) {
}
