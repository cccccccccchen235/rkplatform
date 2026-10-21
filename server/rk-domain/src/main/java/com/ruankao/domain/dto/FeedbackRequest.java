package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 题目反馈提交请求（05 §5.9 第 36 条）。
 *
 * <p>用可变类而非 record：反馈是一次性写入、字段固定，但保持与其它请求 DTO 一致的可变类
 * 风格，便于后续扩展（如截图附件）而不触发构造签名变更。
 *
 * <p>{@code type} 白名单见 {@link com.ruankao.domain.enums.FeedbackType}，其中
 * {@code COPYRIGHT}（版权投诉）在处理端为最高优先级（02 §2.6.3），入口取值必须完整。
 *
 * <p>{@code content} 限制 5~1000 字：下限避免"1 个字的无效反馈"污染处理队列，
 * 上限与管理端展示及字段容量相匹配（05 §5.9）。
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "反馈类型不能为空")
    @Pattern(regexp = "TYPO|WRONG_ANSWER|UNCLEAR_ANALYSIS|COPYRIGHT|OTHER", message = "反馈类型非法")
    private String type;

    @NotBlank(message = "反馈内容不能为空")
    @Size(min = 5, max = 1000, message = "反馈内容需 5~1000 字")
    private String content;
}