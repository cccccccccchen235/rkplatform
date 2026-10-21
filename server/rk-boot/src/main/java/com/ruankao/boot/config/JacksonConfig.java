package com.ruankao.boot.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 时间格式（05 §5.1「时间格式」）。
 *
 * <p><b>为什么必须显式配置</b>：Spring Boot 的默认行为是把 {@code LocalDateTime} 序列化成
 * ISO-8601（{@code 2026-09-27T14:05:03}），而 05 §5.1 的契约是
 * {@code 2026-09-27 14:05:03}——两者只差一个 {@code T}，却是同一份契约在前后端各写一遍的开端。
 * 前端的日期格式化、比大小、以及 {@code types/api.d.ts} 里写下的字符串形状都按契约来，
 * 所以这条格式在服务端一次落实，而不是让每个 Converter 各自 {@code format(...)}。
 *
 * <p>只注册 {@code LocalDateTime}：契约目前只定义了"日期时间"一种格式，
 * {@code LocalDate} / {@code LocalTime} 保持 ISO 默认值，等某个接口真的需要它们时再按契约加，
 * 免得现在先编一个"看起来合理"的格式出来，将来没人敢改。
 *
 * <p>时区由 {@code spring.jackson.time-zone: Asia/Shanghai} 决定（application.yml），
 * 这里只管格式；两处分工写在各自的注释里。
 */
@Configuration
public class JacksonConfig {

    /** 与 05 §5.1 的时间格式逐字符一致。改这里等于改契约，必须同步 05 与前端。 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer rkLocalDateTimeCustomizer() {
        return builder -> builder
                .serializerByType(java.time.LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
                .deserializerByType(java.time.LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
    }
}
