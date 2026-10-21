package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 系统配置（{@code sys_config}），键值对表。
 *
 * <p>{@code uk_config_key(config_key)} 保证配置键唯一；注意本表<b>没有 {@code created_at}</b>，
 * 只有 {@code updated_at}（DDL 见 04 §4.7）——配置只关心改动时间，不需要创建时间。
 *
 * <p>新增配置键时必须同步回 04 §4.8 的初始化清单（对应评审 C3）；启动时的初始化数据见
 * {@code deploy/sql/init-data.sql}。
 */
@Getter
@Setter
@NoArgsConstructor
public class SysConfig {

    private Long id;

    /** 配置键，如 {@code wrongbook.master.threshold}。 */
    private String configKey;

    /** 配置值，以字符串承载（DDL 为 VARCHAR(2000)），取值转换由读取方负责。 */
    private String configValue;

    /** 配置说明；DDL 列名为 {@code remark}（非 {@code description}）。 */
    private String remark;

    private LocalDateTime updatedAt;
}