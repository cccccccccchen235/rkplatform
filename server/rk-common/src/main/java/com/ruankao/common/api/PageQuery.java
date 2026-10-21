package com.ruankao.common.api;

/**
 * 分页请求参数的归一化（05 §5.1）。
 *
 * <p>约定：{@code pageNum} 从 1 开始、默认 1；{@code pageSize} 默认 10、<b>上限 100</b>，
 * 超出按 100 处理。越界值在这里一次性收敛，Service/Mapper 拿到的永远是合法值——
 * 这样"上限 100"这条规则只有一处实现，不会在每个接口里各写一遍且逐渐漂移。
 *
 * <p>注意与"参数校验失败"的区别：{@code pageSize=9999} 不报 1001，而是静默收敛到 100
 * （约束 AC-4 只要求"全部列表接口分页"，并未要求越界即报错）。真正非法的输入
 * （如 {@code pageSize=0} 或负数）同样按默认值处理，不抛异常——分页参数是最不值得
 * 打断用户操作的字段。
 */
public record PageQuery(int pageNum, int pageSize) {

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public static PageQuery of(Integer pageNum, Integer pageSize) {
        return new PageQuery(normalizePageNum(pageNum), normalizePageSize(pageSize));
    }

    public static PageQuery defaultQuery() {
        return new PageQuery(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE);
    }

    private static int normalizePageNum(Integer raw) {
        if (raw == null || raw < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return raw;
    }

    private static int normalizePageSize(Integer raw) {
        if (raw == null || raw < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(raw, MAX_PAGE_SIZE);
    }

    /** MyBatis-Plus / 手写 SQL 的 OFFSET。 */
    public int offset() {
        return (pageNum - 1) * pageSize;
    }

    /** 供 LIMIT 使用的条数，与 {@link #pageSize()} 同值，命名对齐 SQL 语义。 */
    public int limit() {
        return pageSize;
    }
}
