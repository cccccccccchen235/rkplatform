package com.ruankao.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruankao.common.api.FieldErrorVO;
import com.ruankao.common.api.R;
import com.ruankao.fixture.validation.MinCheckedFixtureController;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 参数校验链路的测试（06 §6.3 与 docs/16-p1-code-audit.md §4 E4）。
 *
 * <p>它守的是一类"静默失效"：约束注解写了但不生效（控制器缺 {@code @Validated}），
 * 或者生效了但异常落进兜底分支被报成 {@code 1000 系统内部错误}。两种情况的共同结局都是
 * "用户传了非法参数，却看不到任何有用的提示，运维还多一条 ERROR 堆栈"。
 *
 * <p>用 standalone MockMvc：不启 Spring 容器，因此不依赖数据库、Redis 与环境变量。
 */
class ParamValidationTest {

    private final MinCheckedFixtureController fixture = new MinCheckedFixtureController();

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(fixture)
            .setControllerAdvice(new GlobalExceptionHandler())
            // standalone 默认不装校验器，不显式给一个的话 @RequestBody @Valid 不会校验
            .setValidator(new LocalValidatorFactoryBean())
            .build();

    @Test
    @DisplayName("@RequestBody @Valid 违例 → 1001，字段名是 DTO 属性名")
    void requestBodyViolationMustBeParamInvalid() throws Exception {
        mockMvc.perform(post("/api/fixture/validation/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.message").value("参数校验失败"));
    }

    @Test
    @DisplayName("合法入参不受影响：校验只在违例时拦人，别把正常请求也拦了")
    void validParamMustPassThrough() throws Exception {
        mockMvc.perform(get("/api/fixture/validation/min").param("count", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("count=5"));

        // 只用 ASCII 断言：MockMvc 的 content(String) 默认按 ISO-8859-1 编码请求体，
        // 非 ASCII 会在这条链路上失真，那是测试工具的行为，不是被测代码的问题
        mockMvc.perform(post("/api/fixture/validation/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"soft\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("name=soft"));
    }

    @Test
    @DisplayName("参数类型转换失败（?flag=abc）→ 1001，不是 1000 系统内部错误")
    void typeMismatchMustBeParamInvalid() throws Exception {
        mockMvc.perform(get("/api/fixture/validation/flag").param("flag", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data[0].field").value("flag"));
    }

    /**
     * 直接构造 Spring 6.1 的 {@link HandlerMethodValidationException} 交给生产的处理器。
     *
     * <p>为什么不走 MockMvc：方法级校验是否启用取决于容器怎么装配，standalone MockMvc
     * <b>不</b>注册方法级校验器（实测：{@code @Min(1)} 传 0 会一路进方法体），
     * 所以那条路在单测里造不出真实异常；而<b>这个处理器是所有装配方式下的唯一出口</b>，
     * 它把异常映射成什么，是无论如何都必须成立的。真实容器中的启用情况另由集成验证覆盖。
     */
    @Test
    @DisplayName("方法级校验异常 → 1001，字段名取参数名")
    void handlerMethodValidationMustBeParamInvalid() throws Exception {
        Method method = MinCheckedFixtureController.class.getMethod("min", int.class);

        ResponseEntity<R<List<FieldErrorVO>>> response = new GlobalExceptionHandler()
                .handleHandlerMethodValidation(
                        exceptionFor(new MethodParameter(method, 0), "count 必须大于等于 1"));

        assertEquals(1001, response.getBody().code());
        assertEquals("count", response.getBody().data().get(0).field());
        assertEquals("count 必须大于等于 1", response.getBody().data().get(0).message());
    }

    /**
     * 参数名取不到时的兜底。
     *
     * <p>{@code unnamed} 没有 {@code @RequestParam} 可借名，而这里用的
     * {@code new MethodParameter(method, 0)} <b>没有挂参数名发现器</b>，
     * 所以第二级也取不到（注意：这与"类文件里有没有 {@code MethodParameters}"是两回事，
     * 本项目已开 {@code -parameters}，见下一个测试），只能退回索引形式——
     * <b>不能是 null</b>，否则前端拿到 {@code field: null} 就无法定位输入框。
     */
    @Test
    @DisplayName("参数名与参数注解都取不到时，字段名退回 arg0 而不是 null")
    void handlerMethodValidationFallsBackToIndexName() throws Exception {
        Method method = MinCheckedFixtureController.class.getMethod("unnamed", int.class);

        ResponseEntity<R<List<FieldErrorVO>>> response = new GlobalExceptionHandler()
                .handleHandlerMethodValidation(
                        exceptionFor(new MethodParameter(method, 0), "参数不合法"));

        assertEquals(1001, response.getBody().code());
        assertEquals("arg0", response.getBody().data().get(0).field());
    }

    /**
     * 契约名与形参名不同时，<b>必须以契约为准</b>。
     *
     * <p>这是实机取证后改掉的一处：原先的取值顺序把"编译期形参名"排在最前，
     * 实测（把线上那个 {@code @RequestParam(name = "level") String level} 的形参临时改名为
     * {@code lvl}）返回的 {@code field} 是 {@code lvl}——即客户端发的是 {@code level}，
     * 拿回来的却是 {@code lvl}，前端根本对不上输入框。
     * {@code @RequestParam} / {@code @PathVariable} 声明的名字才是接口契约的一部分。
     */
    @Test
    @DisplayName("契约名优先于形参名：@RequestParam(name=\"wireName\") int javaName → field 是 wireName")
    void declaredParamNameMustWinOverJavaParameterName() throws Exception {
        Method method = MinCheckedFixtureController.class.getMethod("renamed", int.class);

        MethodParameter parameter = new MethodParameter(method, 0);
        // 挂上参数名发现器，模拟 Spring MVC 产出的 MethodParameter（形参名此时可解析为 javaName）
        parameter.initParameterNameDiscovery(new StandardReflectionParameterNameDiscoverer());

        ResponseEntity<R<List<FieldErrorVO>>> response = new GlobalExceptionHandler()
                .handleHandlerMethodValidation(exceptionFor(parameter, "个数必须在1以上"));

        assertEquals(1001, response.getBody().code());
        assertEquals("wireName", response.getBody().data().get(0).field(),
                "字段名取了形参名而不是契约名：客户端发的是 wireName，拿回 javaName 就对不上输入框了");
    }

    /**
     * {@code -parameters} 的编译选项必须保持开启——第 2 级取值完全依赖它。
     *
     * <p>这条断言是"编译选项"的守卫：本项目把 {@code -parameters} 交给了
     * spring-boot-starter-parent 的插件配置，一旦有人换父 POM 或覆写
     * {@code maven-compiler-plugin}，形参名会<b>静默消失</b>（不报错、不警告），
     * 受影响的接口从此返回 {@code arg0} 这类名字。测试会先于用户发现这一点。
     */
    @Test
    @DisplayName("-parameters 必须开启：形参名可从 class 文件解析")
    void javaParameterNamesMustBeCompiledIn() throws Exception {
        Method method = MinCheckedFixtureController.class.getMethod("unnamed", int.class);

        MethodParameter parameter = new MethodParameter(method, 0);
        parameter.initParameterNameDiscovery(new StandardReflectionParameterNameDiscoverer());

        assertEquals("value", parameter.getParameterName(),
                "从 class 文件解析不出形参名，说明 -parameters 编译选项没了"
                        + "（本项目由 spring-boot-starter-parent 提供，见 pom.xml 的 parent）");
    }

    @Test
    @DisplayName("所有控制器必须标 @Validated（06 §6.3：不标则参数上的约束注解静默失效）")
    void everyControllerMustBeValidated() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<BeanDefinition> controllers = scanner.findCandidateComponents("com.ruankao.api");
        assertFalse(controllers.isEmpty(), "扫描根 com.ruankao.api 下一个控制器都没扫到，这道检查已失效");

        for (BeanDefinition definition : controllers) {
            Class<?> controller;
            try {
                controller = Class.forName(definition.getBeanClassName(), false, getClass().getClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("扫描到的类不在测试类路径上：" + definition.getBeanClassName(), ex);
            }
            assertTrue(AnnotatedElementUtils.hasAnnotation(controller, Validated.class),
                    controller.getName() + " 未标注 @Validated。"
                            + "Spring 6.1 的方法级参数校验只在控制器标了 @Validated 时启用，"
                            + "否则 @RequestParam / @PathVariable 上的 @Min / @Size 等约束会静默不生效"
                            + "（不报错、不拦截，非法入参一路进 Service）。");
        }
    }

    /**
     * 造一个与容器产出的形态一致的校验失败结果。
     *
     * <p>为什么用 {@link ParameterErrors} + {@code BindingResult} 而不是
     * {@link ParameterValidationResult}：真实容器装配出来的就是前者（内部持有
     * {@code BindingResult}）。用后者构造虽然更短，但消息会被丢掉，测出来的就不再是
     * 生产路径，而是一个只有测试才会遇到的形态。
     */
    private HandlerMethodValidationException exceptionFor(MethodParameter parameter, String message) {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(fixture, "fixture");
        binding.addError(new ObjectError("param", message));
        ParameterErrors errors = new ParameterErrors(parameter, 0, binding, null, null, null);
        return new HandlerMethodValidationException(MethodValidationResult.create(
                fixture, parameter.getMethod(), List.of(errors)));
    }
}
