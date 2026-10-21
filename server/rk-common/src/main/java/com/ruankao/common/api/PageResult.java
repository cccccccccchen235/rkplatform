package com.ruankao.common.api;

import java.util.List;

/**
 * 分页响应（05 §5.1）。
 *
 * <p>{@code pageNum} / {@code pageSize} 回显请求值（已按上限收敛后的值），
 * 供前端在响应式表格里正确渲染页码。
 *
 * @param list     当前页数据，永不为 null（空页返回空列表而非 null，省掉前端判空）
 * @param total    满足条件的总条数
 * @param pageNum  当前页码，从 1 开始
 * @param pageSize 每页条数
 */
public record PageResult<T>(List<T> list, long total, int pageNum, int pageSize) {

    public PageResult {
        list = list == null ? List.of() : List.copyOf(list);
    }

    public static <T> PageResult<T> of(List<T> list, long total, PageQuery query) {
        return new PageResult<>(list, total, query.pageNum(), query.pageSize());
    }

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), 0L, query.pageNum(), query.pageSize());
    }
}
