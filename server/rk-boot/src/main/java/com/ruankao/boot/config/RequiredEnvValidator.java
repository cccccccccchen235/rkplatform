package com.ruankao.boot.config;

import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 启动期环境变量校验（约束 AC-1：业务代码不得硬编码域名/IP/端口/密钥，
 * 且启动时必须校验必需的环境变量存在）。
 *
 * <p>用 {@link EnvironmentPostProcessor} 而不是 {@code @Component}/{@code InitializingBean}：
 * 它在<b>容器刷新之前</b>执行，缺失变量时连数据源都不会被创建，报错信息就只有这一个原因，
 * 不会掺杂"数据源初始化失败"之类的二次噪音。
 *
 * <p>一次性列出<b>全部</b>缺失项而不是发现第一个就抛：部署时最烦的是"改一个、重启、再报下一个"。
 *
 * <p>只打印键名，<b>绝不打印值</b>——这里处理的都是口令。
 */
public class RequiredEnvValidator implements EnvironmentPostProcessor {

    /**
     * 无安全默认值、必须由环境提供的变量。
     *
     * <p>这份清单与 09 §9.4 环境变量表中标注"是"**且属于应用自身**的条目一致。
     * 09 §9.4 标"是"的共 13 项，比这里多 3 项：{@code MYSQL_ROOT_PASSWORD}、{@code TZ}、
     * {@code JAVA_OPTS}——它们是<b>容器级</b>变量，供 mysql 容器与 {@code docker run} 使用，
     * 应用进程读不到也不需要读。**它们不该成为应用启动的硬门槛**，否则本地开发会被无谓拦住。
     * （即：这张清单与 09 的表格**有意不是 1:1**，比对数量时的差异就是上面这 3 项。）
     *
     * <p>{@code RK_UPLOAD_DIR} 被有意设为必填：P1 尚无上传功能，但"配置目录写错了"
     * 是典型的上线那一刻才暴露的问题（09 §9.12.3 的"上传图片 404"一行），
     * 提前拦住比上线后排查便宜得多。
     *
     * <p>不在此列的（{@code MYSQL_HOST}、{@code MYSQL_PORT}、{@code RK_MGMT_PORT}、
     * {@code RK_LOG_DIR}、{@code RK_CORS_ORIGINS}、{@code RK_DOC_ENABLED}、
     * {@code RK_ACTUATOR_ENABLED}）在 application.yml 里带默认值：
     * 它们取错值不会造成安全问题或连不上库，而缺失会拦住本地开发。
     * 其中 {@code MYSQL_HOST} / {@code MYSQL_PORT} 在 09 §9.4 的表中不出现——
     * compose 里它们是固定值，本地开发默认连 {@code 127.0.0.1:3306}。
     */
    private static final List<String> REQUIRED_KEYS = List.of(
            "MYSQL_DATABASE",
            "MYSQL_USERNAME",
            "MYSQL_PASSWORD",
            "REDIS_HOST",
            "REDIS_PORT",
            "REDIS_PASSWORD",
            "RK_TOKEN_TIMEOUT",
            "RK_SERVER_PORT",
            "RK_LOG_LEVEL",
            "RK_UPLOAD_DIR");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missing = REQUIRED_KEYS.stream()
                .filter(key -> {
                    String value = environment.getProperty(key);
                    return value == null || value.isBlank();
                })
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(buildMessage(missing));
        }
    }

    private String buildMessage(List<String> missing) {
        return """
                启动中止：以下必需环境变量缺失或为空 —— %s

                本项目的环境差异只允许来自环境变量（约束 AC-1），不在代码或配置里写默认值。
                请参考 deploy/.env.example 生成 deploy/.env，或在启动命令中逐个提供：
                    MYSQL_PASSWORD=... REDIS_HOST=... java -jar rk-boot.jar

                注意：MYSQL_PASSWORD 与 REDIS_PASSWORD 只校验"存在且非空"，不校验强度；
                初始口令的生成方式见 09 §9.4.1。"""
                .formatted(String.join(", ", missing));
    }
}
