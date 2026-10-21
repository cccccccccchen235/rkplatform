package com.ruankao.api.fixture;

import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试夹具：方法级 {@code @SaCheckLogin} 的<b>合规</b>控制器（docs/16-p1-code-audit.md §4 E2）。
 *
 * <p>它刻意放在 {@code com.ruankao.api} 之下、但<b>不在</b> {@code com.ruankao.api.controller} 之内：
 * 用来证明扫描根放宽到 {@code com.ruankao.api} 之后，包外的控制器也进入了防线的覆盖范围
 * ——原实现把扫描根写死为 {@code com.ruankao.api.controller}，这个类会被完整漏掉。
 *
 * <p>只存在于测试类路径（{@code src/test}），不会被打进产物，也不会被任何 Spring 上下文扫描到。
 */
@RestController
@RequestMapping("/api/fixture/annotated")
@Validated
public class AnnotatedFixtureController {

    @SaCheckLogin
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
