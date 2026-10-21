package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 知识点合并（05 §5.10 第 50 条）。
 *
 * <p>语义：把 {@code sourceIds} 全部并入 {@code targetId}，即
 * <b>迁移题目关联 → 合并同义词 → 删除 source</b>。整体在一个事务里，
 * 不允许出现"关联迁了一半、标签已删"的中间态。
 *
 * <p>{@code sourceIds} 必须非空：没有来源的合并是一次无操作的写，
 * 只会往审计日志里灌噪音。
 *
 * <p>{@code targetId} 不得出现在 {@code sourceIds} 里（服务层校验）——
 * "把自己合并进自己"会走成"先删自己再往里迁"的删除语义。
 */
public record KnowledgePointMergeRequest(

        @NotEmpty(message = "来源知识点不能为空")
        List<String> sourceIds,

        @NotBlank(message = "目标知识点不能为空")
        String targetId) {
}
