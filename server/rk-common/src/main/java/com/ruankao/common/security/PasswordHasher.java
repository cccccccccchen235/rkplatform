package com.ruankao.common.security;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码哈希（06 §6.5：BCrypt，strength = 10，约 100ms/次）。
 *
 * <p>放在 rk-common 而不是 rk-service：登录（rk-service）与初始化数据的一次性哈希生成器
 * （rk-boot 的 {@code --rk.password-gen}，经 rk-api 的 CLI 调用）必须用<b>同一份实现</b>。
 * 这正是 06 §6.1 第 2 条的理由——用外部工具（如 {@code htpasswd}）产出的是 {@code $2y$} 前缀，
 * 而 Java 侧 BCrypt 只认 {@code $2a$}，结果是一个"哈希看起来没问题、登录一律失败"的哑弹。
 *
 * <p>用 Hutool 的 BCrypt（jBCrypt 的移植）而不是 Spring Security 的
 * {@code BCryptPasswordEncoder}：Hutool 已在技术栈内（03 §3.2），而后者会把
 * spring-security-crypto 及其日志桥接依赖引入最底层模块，只为一个纯函数不值得。
 * 两者产出的都是标准 {@code $2a$} 哈希，将来要换是一行的事。
 */
public final class PasswordHasher {

    /**
     * BCrypt 代价因子。10 约为 100ms/次：足以让离线爆破昂贵，又不至于拖垮登录接口。
     * 调高会让已有哈希失效吗？不会——代价因子存在哈希串自身里，比对本就不需要它。
     */
    public static final int STRENGTH = 10;

    private PasswordHasher() {
    }

    /** 生成哈希。同一明文每次结果都不同（盐随机），故不能用 {@code equals} 比对哈希值。 */
    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(STRENGTH));
    }

    /**
     * 比对明文与存量哈希。
     *
     * <p>哈希串格式非法时返回 {@code false} 而不是抛出异常：{@code init-data.sql} 里
     * 管理员密码是占位串（真实哈希由 {@code --rk.password-gen} 产出后再回填），
     * 格式非法属于"尚未初始化"这一可预期状态，不该变成 500。
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
