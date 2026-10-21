package com.ruankao.domain.enums;

/**
 * 练习模式（{@code GET /practice} 的 {@code mode} 参数，05 §5.5）。
 *
 * <p>本枚举<b>不是一个可直接落库的列</b>，它同时被两个地方引用，且各自的取值集合并不相同：
 * <ul>
 *   <li>{@code user_practice_progress.practice_type}（04 §4.4）只允许
 *       {@code CHAPTER/KP/SEQUENCE/FAVORITE} 四值——错题重做与游客样例都不记进度游标；</li>
 *   <li>{@code user_answer_record.source} 依赖前端模式做映射，见 {@link PracticeSource}
 *       （07 前端上报映射，{@code SAMPLE} 模式为游客、不上报）。</li>
 * </ul>
 *
 * <p>仅 {@code SAMPLE}（游客样例题）<b>免登录</b>，其余模式均需登录：路由守卫在跳转前
 * 按 {@code mode} 判定并干净重定向，不让请求打到后端去吃 {@code 2005}（07 路由表，对应评审 D3）。
 * 各模式取题策略见 06 §6.6.3。
 */
public enum PracticeMode {

    /** 章节练习：按大纲章节抽题（UC-U02）。 */
    CHAPTER,

    /** 知识点练习：按知识点标签抽题（UC-U03）。 */
    KP,

    /** 顺序练习：按题目 {@code id ASC} 连续取题，支持"继续上次"（UC-U04）。 */
    SEQUENCE,

    /** 收藏练习：从收藏夹取题（UC-U06）。 */
    FAVORITE,

    /** 错题重做：关联 {@code user_wrong_question} 中 {@code status='ACTIVE'} 的题（UC-U08）。 */
    WRONG_REDO,

    /** 样例练习：游客免登录试做，题量受限且不落答题记录。 */
    SAMPLE;

    /** 与请求参数比较，避免各处散落 {@code "SAMPLE".equals(...)} 字面量。 */
    public boolean matches(String raw) {
        return name().equals(raw);
    }

    /** 是否允许游客（未登录）进入：仅 {@code SAMPLE} 为 {@code true}。 */
    public boolean isGuestAllowed() {
        return this == SAMPLE;
    }
}