package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户（{@code sys_user}，04 §4.3）。
 *
 * <p>与 {@link Subject} 同样的约定：<b>不带任何 MyBatis 注解</b>，列映射写在 rk-repository 的
 * XML Mapper 里（06 §6.1 禁止 rk-domain 依赖 MyBatis）。
 *
 * <p>{@code passwordHash} 是本项目唯一的敏感字段：它只在本模块与 rk-service 内部流转，
 * <b>绝不进入任何 VO</b>——03 §3.8 的 AC-3 要求响应体与日志都不得出现口令。
 */
@Getter
@Setter
@NoArgsConstructor
public class SysUser {

    private Long id;

    /** 登录名，业务唯一键（{@code uk_username}）。 */
    private String username;

    /** BCrypt(strength=10) 结果，固定 60 字符，见 {@link com.ruankao.common.security.PasswordHasher}。 */
    private String passwordHash;

    private String nickname;

    /** USER / ADMIN。本项目不引入 RBAC 五表模型（01 §1.2）。 */
    private String role;

    /** ACTIVE / DISABLED。禁用账号一律拒绝登录（错误码 2002）。 */
    private String status;

    /** 当前备考科目，影响首页与统计的默认口径（UC-U01）。 */
    private Long currentSubjectId;

    /**
     * 首次登录强制改密（04 §4.3）。
     *
     * <p>用包装类型而不是 {@code boolean}：这一列在 04 的 DDL 里是 {@code TINYINT(1) NOT NULL}，
     * 但将来若出现"部分列查询"（只取 username 与 role），基本类型会静默变成 {@code false}，
     * 把"没查这一列"伪装成"不需要改密"。
     */
    private Boolean mustChangePassword;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
