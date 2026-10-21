package com.ruankao.fixture.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试夹具：参数校验的靶子（docs/16-p1-code-audit.md §4 E4）。
 *
 * <p>三个端点各对应一条必须成立的链路：
 * <ul>
 *   <li>{@code /min}：{@code @RequestParam} 上的约束注解。类上标了 {@code @Validated}，
 *       这是 Spring 6.1 启用方法级校验的前提。</li>
 *   <li>{@code /flag}：参数类型转换失败（{@code ?flag=abc}）——入参不合法，不是服务端故障。</li>
 *   <li>{@code /echo}：{@code @RequestBody @Valid} 的经典分支。</li>
 * </ul>
 * 三条链路失败时都必须映射为 {@code 1001} 而不是 {@code 1000 系统内部错误}。
 *
 * <p>刻意<b>不</b>放进生产扫描根 {@code com.ruankao.api}：它只是测试用的靶子，
 * 不参与鉴权扫描，也不该被 {@code @Validated} 约定检查。由
 * {@code ParamValidationTest} 直接 new 出来交给 standalone MockMvc，不做组件扫描。
 */
@RestController
@RequestMapping("/api/fixture/validation")
@Validated
public class MinCheckedFixtureController {

    @GetMapping("/min")
    public String min(@RequestParam(name = "count") @Min(1) int count) {
        return "count=" + count;
    }

    @GetMapping("/flag")
    public String flag(@RequestParam(name = "flag") boolean flag) {
        return "flag=" + flag;
    }

    @PostMapping("/echo")
    public String echo(@RequestBody @Valid EchoRequest request) {
        return "name=" + request.name();
    }

    /**
     * 仅供测试：一个没有参数注解的方法，用来验证字段名的后两级——
     * 形参名（{@code value}，需参数名发现器）与索引兜底（{@code arg0}）。
     * 它没有任何映射，永远不会被路由。
     */
    public String unnamed(int value) {
        return "value=" + value;
    }

    /**
     * 仅供测试：<b>契约名与形参名刻意不同</b>（{@code wireName} vs {@code javaName}）。
     *
     * <p>这两个名字在正常代码里总是写成同一个，只有故意错开才能判别字段名取的是哪一级。
     * 取契约名才符合预期：那是客户端发出去、也是前端用来定位输入框的名字。
     * 实机验证过两者的差异（把线上那个 {@code @RequestParam(name = "level") String lvl}
     * 的形参改名后，容器返回的 {@code field} 是 {@code lvl} 即形参名——
     * 也就是说改名前，字段名的来源是"编译器参数"而非"接口契约"，那是不对的），
     * 本方法把"必须以契约为准"这条规则钉在测试里。
     */
    @GetMapping("/renamed")
    public String renamed(@RequestParam(name = "wireName") @Min(1) int javaName) {
        return "javaName=" + javaName;
    }

    /** 请求体 DTO：用于验证 {@code @RequestBody @Valid} 分支确实可达。 */
    public record EchoRequest(@NotBlank(message = "name 不能为空") String name) {
    }
}
