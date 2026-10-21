package com.ruankao.domain.vo;

/**
 * 练习作答上报响应（05 §5.5 第 13 条 {@code POST /practice/records}）。
 *
 * <p>{@code correctCount} 是<b>服务端用 {@code JudgeService} 复核后的正确数</b>，
 * 不是客户端报上来的——客户端上报里根本没有对错字段
 * （见 {@link com.ruankao.domain.dto.PracticeRecordRequest}）。
 *
 * @param accepted 成功落库的作答条数
 * @param correctCount 服务端复核后的正确数
 */
public record PracticeRecordResultVO(int accepted, int correctCount) {
}