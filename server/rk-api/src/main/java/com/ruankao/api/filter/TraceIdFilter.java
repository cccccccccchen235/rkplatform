package com.ruankao.api.filter;

import com.ruankao.common.api.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个请求生成 traceId 并注入 MDC（06 §6.3）。
 *
 * <p>取 UUID 的前 8 字节（16 位十六进制），如 {@code 8f2c1a9b4d7e4f10}。
 * 不用完整 UUID：traceId 要出现在每行日志和用户能看到的错误提示里，
 * 32 位的长度对手抄和肉眼比对都不友好，8 字节的碰撞概率在本项目量级下可以忽略。
 *
 * <p>优先级设为最高：它必须早于鉴权拦截器与业务代码，否则前面产生的日志没有 traceId。
 * <b>必须清理 MDC</b>——Tomcat 线程是复用的，不清理会让下一个请求继承上一个请求的 traceId，
 * 这比没有 traceId 更糟：日志会指向错误的请求。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MDC.put(R.TRACE_ID_MDC_KEY, generateTraceId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(R.TRACE_ID_MDC_KEY);
        }
    }

    private static String generateTraceId() {
        return String.format("%016x", UUID.randomUUID().getMostSignificantBits());
    }
}
