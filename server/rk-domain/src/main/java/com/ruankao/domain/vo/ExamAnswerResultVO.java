package com.ruankao.domain.vo;

/** 保存作答的响应（05 §5.6 第 19 条）：返回服务端口径的剩余时长，供前端校准倒计时。 */
public record ExamAnswerResultVO(
        Integer remainingSec,
        Integer currentSittingNo,
        Integer currentSectionNo) {
}
