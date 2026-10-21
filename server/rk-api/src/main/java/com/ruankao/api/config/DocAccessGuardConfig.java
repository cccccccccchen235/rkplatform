package com.ruankao.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 关闭接口文档时，同时让文档页面本身不可达。
 *
 * <p>背景：{@code knife4j.enable=false} 只关掉 Knife4j 的增强能力，
 * {@code springdoc.api-docs.enabled=false} 只关掉 {@code /v3/api-docs} 数据接口，
 * 但打包在 jar 里的 {@code META-INF/resources/doc.html} 与 {@code /webjars/**}
 * 属于静态资源，仍会被资源处理器照常返回——结果是「接口文档开关已关，
 * 页面却能打开，打开后一片报错」。
 *
 * <p>09 §9.3 要求生产必须 {@code RK_DOC_ENABLED=false}，§9.9 的上线核查项写着
 * {@code curl -sSI https://exam.example.com/doc.html} 期望 404。二者都依赖"关掉即不可达"，
 * 而框架的默认行为不是这样，所以在这里补上。
 *
 * <p>实现方式：条件装配。文档关闭时才注册这个拦截器，文档开着时本类根本不加载——
 * 不给"开着文档却拦截了自己"留出可能。判据取 {@code springdoc.api-docs.enabled}，
 * 它与 {@code knife4j.enable} 在 application.yml 里同源于 {@code RK_DOC_ENABLED}，
 * 读它即可，不再新增第三个开关。
 *
 * <p>返回真正的 HTTP 404（而不是统一响应体）：这是一个"页面不存在"的请求，
 * 不是业务应答，5xx/200 都会误导监控与浏览器。
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "false", matchIfMissing = true)
public class DocAccessGuardConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
        }).addPathPatterns("/doc.html", "/doc.html/**", "/webjars/**", "/swagger-ui/**", "/swagger-ui.html");
    }
}
