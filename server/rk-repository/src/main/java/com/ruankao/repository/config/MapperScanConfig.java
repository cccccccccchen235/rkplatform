package com.ruankao.repository.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Mapper 扫描注册。放在 rk-repository 内部，而不是写成 rk-boot 启动类上的
 * {@code @MapperScan("com.ruankao.repository.mapper")}。
 *
 * <p>原因：rk-boot 的分层约束禁止它依赖 rk-repository（06 §6.1），一旦在启动类上写这个注解，
 * 就等于用一个字符串把启动模块和持久层重新绑上——编译期检查不到，重构时也不会报错，
 * 是"约束写在文档里、绕过它却毫无代价"的典型。让持久层自己声明扫描，
 * 这条边就只存在于 rk-repository 内部。
 *
 * <p>为什么不能只靠接口上的 {@code @Mapper} 注解：MyBatis-Plus 的自动扫描用的是
 * Spring Boot 的「自动配置包」，也就是主类所在的 {@code com.ruankao.boot}，
 * 扫不到 {@code com.ruankao.repository.mapper}。{@code @Mapper} 保留在接口上
 * 是为了让 IDE 与静态检查能识别它是 Mapper，注册仍由本类负责。
 */
@Configuration
@MapperScan(basePackages = "com.ruankao.repository.mapper")
public class MapperScanConfig {
}
