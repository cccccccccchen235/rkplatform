package com.ruankao.domain.vo;

/**
 * 题目关联知识点（05 §5.2 的 {@code knowledgePoints[].{id,name}}）。
 *
 * <p>{@code id} 为 {@code String}：同 {@link SubjectVO} 的理由，主键为 {@code BIGINT UNSIGNED}，
 * 超过 2^53 前端 JS 会静默丢精度。
 */
public record QuestionKpVO(String id, String name) {
}