package com.ruankao.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器注册（06 §6.5）。
 *
 * <p>拦截范围 {@code /api/**}，只做"是否登录"这一层粗粒度判断；<b>细粒度权限一律用注解</b>
 * （{@code @SaCheckRole} / {@code @SaCheckPermission}），不在这里堆路径判断。
 *
 * <p>三个要点：
 *
 * <ol>
 *   <li><b>排除项走 Spring MVC 而不是 Sa-Token 的路由匹配</b>：{@code excludePathPatterns}
 *       在拦截器执行之前生效，行为只取决于 Spring 的路径匹配语义，不依赖 Sa-Token 版本内部
 *       如何处理白名单。白名单只有一份，就是 {@link AuthWhitelist#RULES}。</li>
 *   <li><b>{@code /api/v1/practice/papers} 不在排除项里</b>：它允许游客访问，但资格取决于
 *       请求体参数。这里必须让请求进入方法，由方法体按 {@code mode}/{@code count} 判定，
 *       不能让拦截器按路径整条放行（评审 B6）。该方法标注 {@code @SaIgnore}，
 *       依赖 {@code SaInterceptor} 对 {@code @SaIgnore} 的短路返回。
 *       <b>该接口落地时必须补一条集成测试</b>：未登录 + {@code mode=FULL} → {@code 2005}；
 *       未登录 + {@code mode=SAMPLE} 且 {@code count<=5} → 成功（05 §5.13 测试要求）。</li>
 *   <li><b>不用 {@code SaRouter.match(...).notMatch(...)}</b>：那套匹配器与 Spring 的
 *       Ant 语义在 {@code /**} 是否匹配基路径上不一致，双套语义是最容易长出安全洞的地方。</li>
 * </ol>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(AuthWhitelist.pathPatterns());
    }
}
