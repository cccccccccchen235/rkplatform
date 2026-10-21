package com.ruankao.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 在线接口文档（Knife4j / OpenAPI 3）。
 *
 * <p>只在开发与测试环境开放：生产环境由 {@code RK_DOC_ENABLED=false} 关闭
 * （09 §9.x 环境变量表），关闭后 {@code /v3/api-docs} 与 {@code /doc.html} 都不存在。
 * 接口文档暴露的字段结构对攻击者是免费的侦察材料。
 *
 * <p>注意"关闭"分两处：本类与 application.yml 的开关关掉的是文档的数据接口，
 * 而打包在 jar 里的静态页面需要 {@link DocAccessGuardConfig} 才会真正 404。
 *
 * <p>这里只放一点点元信息，接口的标题与说明写在各自的 {@code @Tag}/{@code @Operation} 上，
 * 与 05-api.md 的章节号对应，评审时可以直接对照。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI ruankaoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("软考在线学习平台 API")
                        .description("路径前缀 /api/v1；统一响应体见 docs/05-api.md §5.1.1，错误码见 §5.1.2。")
                        .version("v1")
                        .license(new License().name("内部项目")));
    }
}
