package com.ruankao.boot;

import com.ruankao.api.cli.PasswordGenCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。全项目唯一的 {@code main}（06 §6.1）。
 *
 * <p>组件扫描范围显式写成 {@code com.ruankao}：主类在 {@code com.ruankao.boot}，
 * 默认扫描范围只有这一个包，控制器、Service、Mapper 全都扫不到。
 */
@SpringBootApplication(scanBasePackages = "com.ruankao")
@EnableScheduling
public class RkBootApplication {

    public static void main(String[] args) {
        // 一次性 CLI（密码哈希）在 Spring 之前分派：它既不该启动 Web 容器，
        // 也不该被 AC-1 的环境变量校验拦住（给数据库准备密码的人还没配好数据库）。
        if (PasswordGenCli.tryRun(args)) {
            return;
        }
        SpringApplication.run(RkBootApplication.class, args);
    }
}
