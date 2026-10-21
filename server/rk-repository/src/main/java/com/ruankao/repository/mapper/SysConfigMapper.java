package com.ruankao.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置读取（{@code sys_config}，DDL 见 04 §4.7，初始化清单见 04 §4.8）。
 *
 * <p>模板参数化配置一律从本表读取，<b>不在代码里写死</b>，例如错题本掌握阈值
 * {@code wrongbook.master.threshold}（06 §6.8）、单题耗时上限
 * {@code judge.max.duration.ms}（04 §4.3）。本轮只提供"按键取值"一种读法：
 * 调用方拿到字符串后自行转换类型（DDL 为 {@code VARCHAR(2000)}，取值转换归读取方，
 * 见 {@link com.ruankao.domain.entity.SysConfig} 的说明）。
 *
 * <p>新增配置键时必须同步回 04 §4.8 的初始化清单与 {@code deploy/sql/init-data.sql}
 * （对应评审 C3），本 Mapper 不负责写入。
 *
 * <p>SQL 写在 {@code resources/mapper/SysConfigMapper.xml}。
 */
@Mapper
public interface SysConfigMapper {

    /**
     * 按配置键取配置值。
     *
     * @return 配置值字符串；<b>键不存在时返回 {@code null}</b>，由调用方决定回退到
     *         代码默认值还是报错（例如 {@code wrongbook.master.threshold} 缺失时回退到 2）
     */
    String selectValueByKey(@Param("configKey") String configKey);
}