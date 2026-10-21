package com.ruankao.fixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试夹具：<b>故意</b>不标任何鉴权注解的控制器（docs/16-p1-code-audit.md §4 E2 要求的"自测"）。
 *
 * <p>为什么它不是"随手写的坏代码"，而是必须存在的证据：鉴权反射扫描是一道防线，
 * 但没人知道它是不是真的连着电——除非把一个已知违规的控制器放进去，断言它确实会被判为违规。
 * 断言写在 {@code AuthAnnotationTest#unannotatedControllerMustBeDetected}。
 *
 * <p>它刻意放在扫描根 {@code com.ruankao.api} <b>之外</b>（本包 {@code com.ruankao.fixture} 是它的兄弟包）：
 * 这样常规扫描测试不会被它弄红，只有显式扫描本包的自测能看到它。若将来把扫描根改成
 * {@code com.ruankao}，这个类会让常规测试失败——那时应把它移到扫描根之外，而不是删掉断言。
 *
 * <p>只存在于测试类路径，不会被打进产物。
 */
@RestController
@RequestMapping("/api/fixture/secret")
public class UnannotatedFixtureController {

    @GetMapping
    public String secret() {
        return "should never be exposed";
    }
}
