package com.ruankao.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 锁定密码哈希的三个对外契约。它们各自被别处依赖，坏掉时不会在本模块报错，
 * 而是在很远的地方以"登录一律失败"的形式出现：
 *
 * <ul>
 *   <li><b>前缀 {@code $2a$}</b> —— 09 §9.9 的决策：不用 {@code htpasswd} 生成哈希，
 *       因为 {@code apache2-utils} 产出 {@code $2y$} 前缀，而 Java 侧 BCrypt 按
 *       {@code $2a$} 分支解析。生成与校验必须是同一实现。</li>
 *   <li><b>长度 60</b> —— 09 §9.9 第 7 步的核查语句是
 *       {@code LENGTH(password_hash) AS hash_len}，注解写着"必须 60"，
 *       与 {@code sys_user.password_hash CHAR(60)} 对应。超长会被静默截断。</li>
 *   <li><b>代价因子 10</b> —— 06 §6.5 定的值。</li>
 * </ul>
 *
 * <p>还有一条容易被忽略的行为：比对"占位哈希"（{@code init-data.sql} 里未替换的占位串）
 * 必须返回 false 而不是抛异常——否则未初始化的环境会以 500 收场。
 */
class PasswordHasherTest {

    @Test
    @DisplayName("哈希为 $2a$10$ 前缀、60 字符，与 sys_user.password_hash CHAR(60) 对齐")
    void hashFormatMatchesSchemaContract() {
        String hash = PasswordHasher.hash("ChangeMe@2026");

        assertTrue(hash.startsWith("$2a$10$"), "前缀必须是 $2a$10$，实际：" + hash);
        assertEquals(60, hash.length(), "长度必须是 60，实际 " + hash.length() + "：" + hash);
        assertEquals(10, PasswordHasher.STRENGTH);
    }

    @Test
    @DisplayName("同一明文两次哈希不同（随机盐），但都能比对通过")
    void saltIsRandomButVerificationWorks() {
        String first = PasswordHasher.hash("ChangeMe@2026");
        String second = PasswordHasher.hash("ChangeMe@2026");

        assertNotEquals(first, second, "盐随机 => 同一明文两次哈希不应相同");
        assertTrue(PasswordHasher.matches("ChangeMe@2026", first));
        assertTrue(PasswordHasher.matches("ChangeMe@2026", second));
    }

    @Test
    @DisplayName("错误口令与占位哈希都返回 false，且不抛异常")
    void mismatchesReturnFalseWithoutThrowing() {
        String hash = PasswordHasher.hash("ChangeMe@2026");

        assertFalse(PasswordHasher.matches("ChangeMe@2027", hash));
        assertFalse(PasswordHasher.matches("", hash));
        assertFalse(PasswordHasher.matches(null, hash));
        assertFalse(PasswordHasher.matches("ChangeMe@2026", null));

        // init-data.sql 里尚未替换的占位串：格式非法，必须安静地判为不匹配
        assertFalse(PasswordHasher.matches("ChangeMe@2026", "<BCrypt占位，待 rk-boot 的哈希生成器产出>"));
    }
}
