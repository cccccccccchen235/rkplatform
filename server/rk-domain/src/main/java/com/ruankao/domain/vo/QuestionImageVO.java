package com.ruankao.domain.vo;

/**
 * 题干插图（05 §5.2 的 {@code images[].{url,alt}}）。
 *
 * <p>{@code url} 为可访问地址，{@code alt} 为无障碍替代文本（可为 {@code null}）。
 */
public record QuestionImageVO(String url, String alt) {
}