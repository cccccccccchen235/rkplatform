package com.ruankao.domain.vo;

/** 启动场次的响应（05 §5.6 第 20 条）。 */
public record ExamSittingStartVO(
        ExamSittingVO sitting,
        Integer remainingSec,
        Integer currentSectionNo) {
}
