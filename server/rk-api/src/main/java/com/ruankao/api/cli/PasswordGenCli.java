package com.ruankao.api.cli;

import com.ruankao.common.security.PasswordHasher;

/**
 * 一次性命令行工具：由明文口令产出 BCrypt 哈希（06 §6.1 第 2 条）。
 *
 * <pre>
 *   java -jar rk-boot.jar --rk.password-gen='明文口令'
 * </pre>
 *
 * <p>用途：给 {@code deploy/sql/init-data.sql} 的管理员账号产出真实哈希。此前那里是占位串，
 * 导致 09 §9.13 的"初始密码登录"核查项无法执行。
 *
 * <p>三条设计约束：
 *
 * <ol>
 *   <li><b>不启动 Spring</b>。入口在 rk-boot 的 {@code main} 里、{@code SpringApplication.run}
 *       之前分派，所以既不启动 Web 容器，也不触发 AC-1 的环境变量校验——
 *       给数据库准备密码的人不该先被要求配好数据库连接。</li>
 *   <li><b>只输出哈希，不回显明文</b>。明文已经因为写在命令行上而进了 shell history，
 *       再往 stdout 打一遍只会扩大暴露面。用完请清理 shell 历史。</li>
 *   <li><b>实现放在 rk-api 而非 rk-boot</b>。rk-boot 的分层约束只允许依赖 rk-api
 *       （06 §6.1），而这里需要 rk-common 的 {@link PasswordHasher}；
 *       放在 rk-api 就能沿"rk-boot → rk-api → rk-common"的合法路径拿到，
 *       不必为一次性的 CLI 破例放宽启动模块的依赖约束。</li>
 * </ol>
 */
public final class PasswordGenCli {

    /** 参数前缀。用 {@code --key=value} 形式而非位置参数，避免与 Spring 的参数解析冲突。 */
    public static final String ARG_PREFIX = "--rk.password-gen=";

    private PasswordGenCli() {
    }

    /**
     * 若本次是哈希生成调用则执行并返回 {@code true}，调用方应随即退出、不再启动 Spring。
     *
     * @param args 原始命令行参数
     * @return true 表示已按 CLI 处理（无论成功或用法错误），调用方必须直接返回
     */
    public static boolean tryRun(String[] args) {
        String rawPassword = findValue(args);
        if (rawPassword == null) {
            return false;
        }
        if (rawPassword.isBlank()) {
            System.err.println("明文口令为空。用法：java -jar rk-boot.jar --rk.password-gen='<明文口令>'");
            // 返回 true：这是"处理过了"，不该退回去启动 Web 容器
            return true;
        }
        System.out.println(PasswordHasher.hash(rawPassword));
        return true;
    }

    private static String findValue(String[] args) {
        if (args == null) {
            return null;
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith(ARG_PREFIX)) {
                return arg.substring(ARG_PREFIX.length());
            }
        }
        return null;
    }
}
