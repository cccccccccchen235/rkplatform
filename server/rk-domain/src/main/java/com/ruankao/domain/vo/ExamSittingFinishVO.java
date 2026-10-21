package com.ruankao.domain.vo;

import java.util.List;

/** 提前结束场次的响应（05 §5.6 第 22 条）。 */
public record ExamSittingFinishVO(
        Integer currentSittingNo,
        Integer remainingSec,
        List<ExamSittingVO> sittings) {
}
