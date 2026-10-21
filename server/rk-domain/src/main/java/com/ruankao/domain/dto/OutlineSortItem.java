package com.ruankao.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 章节拖拽排序的一项（05 §5.10 第 45 条，请求体是这类对象的数组）。
 *
 * <p>同时携带 {@code parentId}：管理端的一次拖拽可能**跨层**（把二级章节拖到另一个
 * 一级下，或拖成根），所以排序接口必须能表达"换父节点"，否则用户只能删了重建。
 *
 * <p>{@code parentId} 与 {@code sortNo} 都必填：这是一次**全量覆盖式**提交
 * （管理端提交的是当前树的完整顺序）。允许缺省会出现"没传的字段保持原值"这种
 * 半更新语义，而排序恰恰是最经不起一半新一半旧的操作——树形结构里一行没跟上，
 * 整棵树的渲染顺序就错了。
 */
@Getter
@Setter
@NoArgsConstructor
public class OutlineSortItem {

    @NotBlank(message = "节点 ID 不能为空")
    private String id;

    /** 新父节点；{@code "0"} = 根。 */
    @NotBlank(message = "父节点 ID 不能为空")
    private String parentId;

    @NotNull(message = "排序号不能为空")
    @Min(value = 0, message = "排序号不能为负")
    private Integer sortNo;
}
