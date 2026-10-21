package com.ruankao.domain.enums;

/**
 * 科目级别（{@code rk_subject.level}）。
 *
 * <p>{@code GET /subjects} 按本枚举的三个取值分组返回（05 §5.4），
 * 响应体的三个 key 恒存在——空组返回空数组而非省略，省掉前端的三态处理。
 *
 * <p>枚举<b>声明顺序即响应体 key 的顺序</b>：初级 → 中级 → 高级，与页面的学习路径一致。
 */
public enum SubjectLevel {

    /** 初级 */
    JUNIOR,

    /** 中级 */
    MIDDLE,

    /** 高级 */
    SENIOR;

    /** 与库中字符串比较，避免各处散落 {@code "JUNIOR".equals(...)} 字面量。 */
    public boolean matches(String raw) {
        return name().equals(raw);
    }
}
