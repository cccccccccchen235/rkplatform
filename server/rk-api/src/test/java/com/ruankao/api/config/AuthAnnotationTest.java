package com.ruankao.api.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-02：接口鉴权的反射扫描测试（06 §6.5）。
 *
 * <p>它守的是"隐式安全"这道口子：<b>不允许存在"靠全局拦截器兜底"的接口</b>。
 * 只有两种合法写法——在白名单里（整条路径公开），或者方法（或类）上标注
 * {@code @SaCheckLogin} / {@code @SaCheckRole} / {@code @SaCheckPermission} / {@code @SaIgnore}。
 * 少了任何一个，新接口就可能悄悄对外裸奔，而这是代码评审最容易漏看的一类问题。
 *
 * <p>纯反射，不起 Spring 容器，因此不依赖数据库、Redis 与环境变量。
 *
 * <p>两处曾经的缺陷已修（docs/16-p1-code-audit.md §4 E1/E2），并由自测锁定：
 * ① 判定只看方法、看不到类级注解，且报错文案把人推向"登记白名单"这条真漏洞；
 * ② 扫描根写死为 {@code com.ruankao.api.controller}，包外的控制器完全逃逸。
 */
class AuthAnnotationTest {

    /**
     * 控制器扫描根（06 §6.2 的包结构约定：控制器必须放在 {@code com.ruankao.api} 之内）。
     *
     * <p>为什么是 {@code com.ruankao.api} 而不是 {@code com.ruankao.api.controller}：
     * 后者是"当前控制器的位置"，不是"控制器必须在的位置"——包外新起的 {@code @RestController}
     * 同样会被 Spring 注册成接口，却完全逃过本测试。扫描根必须大于等于包结构约定的覆盖面，
     * 否则这道防线会在无人察觉时失效。
     */
    private static final String CONTROLLER_BASE_PACKAGE = "com.ruankao.api";

    /** 夹具包（测试专用，位于扫描根之内、controller 之外）。 */
    private static final String FIXTURE_PACKAGE = "com.ruankao.api.fixture";

    /** 故意违规的夹具包（测试专用，刻意在扫描根之外，见该类的注释）。 */
    private static final String UNANNOTATED_FIXTURE_PACKAGE = "com.ruankao.fixture";

    private static final String UNANNOTATED_FIXTURE_CLASS =
            "com.ruankao.fixture.UnannotatedFixtureController";

    private static final List<Class<? extends java.lang.annotation.Annotation>> AUTH_ANNOTATIONS = List.of(
            SaCheckLogin.class, SaCheckRole.class, SaCheckPermission.class, SaIgnore.class);

    @Test
    @DisplayName("每个接口方法要么在白名单内，要么显式声明鉴权注解")
    void allControllerMethodsMustDeclareAuth() {
        List<String> violations = collectViolations(scanControllers(CONTROLLER_BASE_PACKAGE));

        assertTrue(violations.isEmpty(), () -> """
                以下接口方法既未标注 @SaCheckLogin / @SaCheckRole / @SaCheckPermission / @SaIgnore，
                也不在 AuthWhitelist 中（ADR-02 禁止"靠全局拦截器兜底"的接口）：

                  %s

                两种修法选其一：
                  1) 确实需要登录 → 在方法（或类）上加 @SaCheckLogin 或 @SaCheckRole("ADMIN")；
                  2) 确实要完全公开 → **仅当该接口在无登录时被误加/改请求参数也不会出问题**，
                     才允许在 AuthWhitelist.RULES 中登记「HTTP 方法 + 路径模式」，并把理由
                     写进该方法或该常量的注释（05 §5.13）。
                登记白名单不是"让测试变绿"的手段：把本该鉴权的路径写进去，测试会通过，
                但那就是一个真漏洞。判断不了就选第 1 种。
                注意：条件性放行（如"仅 mode=SAMPLE 时允许游客"）不能写进白名单，
                必须标注 @SaIgnore 后在方法体内判断（评审 B6）。
                """.formatted(String.join("\n  ", violations)));
    }

    @Test
    @DisplayName("扫描根必须覆盖 controller 之外的包（E2：包外控制器不得逃逸）")
    void scanRootMustCoverPackagesOutsideController() {
        String fixtureClass = FIXTURE_PACKAGE + ".AnnotatedFixtureController";

        assertTrue(beanClassNames(CONTROLLER_BASE_PACKAGE).contains(fixtureClass),
                "扫描根 " + CONTROLLER_BASE_PACKAGE + " 没扫到 " + fixtureClass
                        + "；扫描根若小于 06 §6.2 的包结构约定，包外的控制器就会逃过鉴权检查");

        assertFalse(beanClassNames("com.ruankao.api.controller").contains(fixtureClass),
                "把扫描根写死为 com.ruankao.api.controller 时会漏掉这个控制器"
                        + "——这正是 E2 修掉的口子，别把它改回去");
    }

    @Test
    @DisplayName("类级鉴权注解必须被识别（E1：按提示标在类上的人不该再看到违规）")
    void classLevelAnnotationMustBeCovered() {
        List<String> violations = collectViolations(scanControllers(FIXTURE_PACKAGE));
        assertTrue(violations.isEmpty(),
                "类级 @SaCheckRole(\"ADMIN\") 未被识别。Sa-Token 支持类级注解，"
                        + "Java 反射的 isAnnotationPresent 却不会向上读声明类的注解——"
                        + "漏掉这一步的后果是：按提示在类上标注的人依然看到违规，"
                        + "然后被文案的第二种修法推向「登记白名单」这条真漏洞。实际违规：" + violations);
    }

    @Test
    @DisplayName("未标注的控制器必须被判为违规（这道防线必须真的带电）")
    void unannotatedControllerMustBeDetected() {
        List<String> violations = collectViolations(scanControllers(UNANNOTATED_FIXTURE_PACKAGE));

        assertFalse(violations.isEmpty(),
                "把 " + UNANNOTATED_FIXTURE_CLASS + " 交给判定却没报违规——"
                        + "说明扫描或判定已经失效，此时 allControllerMethodsMustDeclareAuth 的绿灯毫无意义");
        assertTrue(violations.stream().anyMatch(v -> v.contains("UnannotatedFixtureController")),
                "违规列表里没有那个已知违规的夹具：" + violations);
    }

    @Test
    @DisplayName("practice/papers 不得进入白名单（评审 B6：白名单只能表达路径级放行）")
    void practicePapersMustNotBeWhitelisted() {
        assertFalse(AuthWhitelist.isWhitelisted("POST", AuthWhitelist.PRACTICE_PAPERS_PATH),
                "POST /api/v1/practice/papers 一旦进入白名单，游客即可用 mode=FULL 绕过题量限制。"
                        + "该方法应标注 @SaIgnore 并在方法体内校验 mode=SAMPLE 且 count<=5。");
    }

    @Test
    @DisplayName("白名单按 HTTP 方法匹配，不是只按路径放行")
    void whitelistMustMatchHttpMethod() {
        assertTrue(AuthWhitelist.isWhitelisted("GET", "/api/v1/subjects"));
        assertTrue(AuthWhitelist.isWhitelisted("GET", "/api/v1/subjects/12/outline"));

        // 同一个路径换个方法就必须鉴权，否则"只读公开"会静默变成"可写公开"
        assertFalse(AuthWhitelist.isWhitelisted("POST", "/api/v1/subjects"));
        assertFalse(AuthWhitelist.isWhitelisted("DELETE", "/api/v1/subjects/12"));
        // 管理端整体要求 role=ADMIN，任何一条落到白名单里都是漏网
        assertFalse(AuthWhitelist.isWhitelisted("GET", "/api/v1/admin/users"));
        assertFalse(AuthWhitelist.isWhitelisted("GET", "/api/v1/exams/1"));
    }

    private Set<BeanDefinition> scanControllers(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents(basePackage);
    }

    private Set<String> beanClassNames(String basePackage) {
        return scanControllers(basePackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .collect(Collectors.toSet());
    }

    /**
     * 扫描指定包并逐方法判定，返回违规清单。
     *
     * <p>抽成纯函数是为了让自测能拿"已知违规的夹具"去喂它：只有这样才能证明这道防线是带电的
     * （见 {@link #unannotatedControllerMustBeDetected}）。
     */
    private List<String> collectViolations(Set<BeanDefinition> controllers) {
        List<String> violations = new ArrayList<>();

        for (BeanDefinition definition : controllers) {
            Class<?> controller;
            try {
                // initialize=false：只要元数据与注解，不触发静态初始化
                controller = Class.forName(definition.getBeanClassName(), false, getClass().getClassLoader());
            } catch (ClassNotFoundException ex) {
                // 不吞掉：漏扫一个控制器就等于给防线开一个口子，必须响亮地失败
                throw new IllegalStateException("扫描到的类不在测试类路径上：" + definition.getBeanClassName(), ex);
            }

            String[] prefixes = classLevelPaths(controller);
            for (Method method : controller.getDeclaredMethods()) {
                if (!isEndpoint(method)) {
                    continue;
                }
                if (hasAuthAnnotation(method)) {
                    continue;
                }
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                List<String> uncovered = uncoveredMappings(prefixes, mapping);
                if (!uncovered.isEmpty()) {
                    violations.add(controller.getName() + "#" + method.getName() + " -> " + uncovered);
                }
            }
        }
        return violations;
    }

    /** 只检查真正的接口方法：public、非静态、非合成/桥接。 */
    private boolean isEndpoint(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers)
                && !Modifier.isStatic(modifiers)
                && !method.isSynthetic()
                && !method.isBridge();
    }

    /**
     * 方法<b>或其声明类</b>上有任一鉴权注解即算已覆盖。
     *
     * <p>两处都不能省：
     * <ul>
     *   <li>用 {@code AnnotatedElementUtils.hasAnnotation} 而非 {@code isAnnotationPresent}：
     *       前者会递归处理元注解，后者只看直接标注的注解。</li>
     *   <li>查声明类：Sa-Token 的注解校验器<b>支持类级注解</b>（管理端控制器标
     *       {@code @SaCheckRole("ADMIN")} 是惯用写法），而 Java 反射不会向上读声明类的注解。
     *       漏了这一步，按提示把注解标在类上的人会依然看到违规（docs/16 §4 E1）。</li>
     * </ul>
     */
    private boolean hasAuthAnnotation(Method method) {
        return AUTH_ANNOTATIONS.stream().anyMatch(annotation ->
                AnnotatedElementUtils.hasAnnotation(method, annotation)
                        || AnnotatedElementUtils.hasAnnotation(method.getDeclaringClass(), annotation));
    }

    /** 类级 {@code @RequestMapping} 前缀；未声明时返回空串。 */
    private String[] classLevelPaths(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        if (mapping == null) {
            return new String[]{""};
        }
        String[] paths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        return paths.length > 0 ? paths : new String[]{""};
    }

    /**
     * 找出既不在白名单内、又缺鉴权注解的「方法 + 路径」组合。
     *
     * <p>判定保守：只要有一个组合没被白名单覆盖就报违规。
     * 例如某方法同时映射 {@code GET /subjects} 与 {@code POST /subjects}，
     * 而白名单只放了 GET，那么它必须显式标注注解，不能靠"另一个方法在白名单里"混过去。
     */
    private List<String> uncoveredMappings(String[] prefixes, RequestMapping mapping) {
        List<String> uncovered = new ArrayList<>();
        if (mapping == null) {
            return uncovered;
        }
        RequestMethod[] httpMethods = mapping.method();
        if (httpMethods.length == 0) {
            // 未限定 HTTP 方法 = 接受任意方法，而白名单按方法登记，故无法被覆盖
            uncovered.add("(未限定 HTTP 方法) " + String.join(", ", combine(prefixes, mapping)));
            return uncovered;
        }
        String[] paths = combine(prefixes, mapping);
        for (RequestMethod httpMethod : httpMethods) {
            for (String path : paths) {
                if (!AuthWhitelist.isWhitelisted(httpMethod.name(), path)) {
                    uncovered.add(httpMethod.name() + " " + path);
                }
            }
        }
        return uncovered;
    }

    /** 拼接类级前缀与方法级路径，收敛重复与缺失的斜杠。 */
    private String[] combine(String[] prefixes, RequestMapping mapping) {
        String[] methodPaths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        if (methodPaths.length == 0) {
            methodPaths = new String[]{""};
        }
        List<String> result = new ArrayList<>();
        for (String prefix : prefixes) {
            for (String methodPath : methodPaths) {
                result.add(("/" + trimSlashes(prefix) + "/" + trimSlashes(methodPath)).replaceAll("/{2,}", "/"));
            }
        }
        return result.toArray(String[]::new);
    }

    private String trimSlashes(String raw) {
        return raw == null ? "" : raw.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
