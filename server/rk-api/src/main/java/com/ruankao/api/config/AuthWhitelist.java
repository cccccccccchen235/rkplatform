package com.ruankao.api.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.AntPathMatcher;

/**
 * 鉴权白名单（05 §5.13）。
 *
 * <p><b>这份常量是唯一事实来源</b>：拦截器与 ADR-02 的鉴权单元测试共用它，避免规则在两处漂移
 * （06 §6.5）。
 *
 * <p><b>它只能表达"整条路径放行"，永远不表达"满足某条件才放行"。</b>
 * 典型反例见 {@link #PRACTICE_PAPERS_PATH}：该路径允许游客访问，但资格取决于请求体里的
 * {@code mode} 与 {@code count}，属于参数级判断，只能写在方法体内。
 * 一旦把它加进本白名单，游客就能用 {@code mode=FULL} 绕过题量限制（评审 B6，安全洞）。
 */
public final class AuthWhitelist {

    /**
     * 游客可创建练习卷的路径。<b>刻意不在白名单内</b>：
     * 该方法标注 {@code @SaIgnore}，游客资格在方法体内校验（{@code mode=SAMPLE} 且 {@code count<=5}），
     * 否则抛 {@code 2005}。此处保留常量是为了让"它为什么不该出现在下面"这句话有落脚点，
     * 并由 {@code AuthAnnotationTest} 断言它不在白名单中。
     */
    public static final String PRACTICE_PAPERS_PATH = "/api/v1/practice/papers";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 一条白名单规则：HTTP 方法 + 路径模式。方法必须匹配，不能只按路径放行。 */
    public record Rule(String httpMethod, String pathPattern) {
    }

    /**
     * 无需登录即可访问的接口（05 §5.13 逐条对应）。
     *
     * <p>说明两处：
     * <ul>
     *   <li>{@code GET /api/v1/subjects}（不带后缀）是<b>额外补充</b>的：Ant 语义下
     *       {@code /api/v1/subjects/**} 不匹配基路径本身，而科目列表正是基路径。
     *       05 §5.13 用 {@code /**} 表达"该前缀下全部"，这里把它展开成两种写法。</li>
     *   <li>{@code /{id}/outline} 与 {@code /{id}/knowledge-points} 已被
     *       {@code /api/v1/subjects/**} 覆盖，保留是因为 05 §5.13 逐条列出了它们，
     *       评审时可以直接与文档对照。</li>
     * </ul>
     */
    public static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/v1/auth/register"),
            new Rule("POST", "/api/v1/auth/login"),
            new Rule("GET", "/api/v1/subjects"),
            new Rule("GET", "/api/v1/subjects/**"),
            new Rule("GET", "/api/v1/subjects/{id}/outline"),
            new Rule("GET", "/api/v1/subjects/{id}/knowledge-points"),
            // Knife4j。生产环境由 RK_DOC_ENABLED=false 关闭，届时这些路径本身就不存在；
            // 此处放行的是"文档站点不该要求登录"，而不是"文档站点始终开放"。
            new Rule("GET", "/doc.html"),
            new Rule("GET", "/webjars/**"),
            new Rule("GET", "/v3/api-docs/**"));

    private AuthWhitelist() {
    }

    /** 供 Spring MVC 的 {@code excludePathPatterns} 使用。 */
    public static String[] pathPatterns() {
        return RULES.stream().map(Rule::pathPattern).distinct().toArray(String[]::new);
    }

    /** 供 ADR-02 单元测试与调试使用：判断某个方法与路径是否被放行。 */
    public static boolean isWhitelisted(String httpMethod, String path) {
        if (httpMethod == null || path == null) {
            return false;
        }
        String method = httpMethod.toUpperCase();
        return RULES.stream()
                .anyMatch(rule -> rule.httpMethod().equals(method) && MATCHER.match(rule.pathPattern(), path));
    }

    /** 白名单覆盖的路径模式，测试用来判断"控制器方法是否落在放行前缀下"。 */
    public static Set<String> patternsForMethod(String httpMethod) {
        String method = httpMethod == null ? "" : httpMethod.toUpperCase();
        return RULES.stream()
                .filter(rule -> rule.httpMethod().equals(method))
                .map(Rule::pathPattern)
                .collect(Collectors.toUnmodifiableSet());
    }
}
