package com.ruankao.domain.vo;

/**
 * 题目选项（05 §5.2 的 {@code options[].{key,content}}）。
 *
 * <p>{@code key} 是选项字母（A~F），{@code content} 是选项正文（Markdown + KaTeX）。
 * record，字段即契约；仅客观题下发，主观题的 {@code options} 为 {@code null}（见 {@link QuestionVO}）。
 */
public record QuestionOptionVO(String key, String content) {
}