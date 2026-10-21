package com.ruankao.domain.vo;

import java.util.List;

/**
 * 练习取卷响应（05 §5.5 第 12 条 {@code POST /practice/papers}）。
 *
 * <p>{@code nextSequence} 供 {@code SEQUENCE} 模式续做：它是本次卷面之后的下一个题序游标，
 * 前端完成本卷后据此上报练习进度即可。非顺序模式该值虽返回但无实际意义，
 * 统一返回可避免前端按模式写取值分支。
 *
 * @param questions 本次返回的题目（详情形态，但取卷时不含本次作答结果）
 * @param total 本次返回的题目总数
 */
public record PracticePaperVO(List<QuestionDetailVO> questions, int total, int nextSequence) {
}