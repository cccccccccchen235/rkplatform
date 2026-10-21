package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 操作审计日志（{@code sys_operation_log}），03 §3.8 的 <b>AC-2</b> 承载表。
 *
 * <p>AC-2 要求"高危操作必须留痕"。P2 引入的第一个高危操作是<b>科目停用</b>
 * （= 下架，23 §3.2），此前这张表一直没有写入方。
 *
 * <p>{@code module} 与 {@code action} 的取值是 DDL 注释里列出的枚举，
 * 本类<b>不做枚举化</b>：它们是审计字段，取值集合会随功能增长，
 * 用枚举会让"加一个新 action"变成一次跨模块改动，而审计的价值在于
 * "当时写进去的原文"，不在于类型安全。
 *
 * <p><b>绝不写密码、token、答案正文</b>（AC-3）：{@code detail} 只放业务键与结果摘要。
 */
@Getter
@Setter
@NoArgsConstructor
public class SysOperationLog {

    private Long id;

    /** 操作者用户 ID（AC-3：用户数据访问必须携带 user_id）。 */
    private Long userId;

    /** QUESTION / REVIEW / SUBJECT / USER / CONFIG。 */
    private String module;

    /** CREATE / UPDATE / DELETE / PUBLISH / OFFLINE / RESET_PASSWORD / DISABLE。 */
    private String action;

    private String targetType;

    private Long targetId;

    /** 结果摘要；不超过 1000 字符（DDL 列宽）。 */
    private String detail;

    /** 兼容 IPv6，故 45 字符宽。 */
    private String ip;

    private LocalDateTime createdAt;
}
