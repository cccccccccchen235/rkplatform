package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 科目启停（05 §5.10 第 40 条）。
 *
 * <p>只有 {@code status} 一个字段，独立成一个 DTO 而不复用
 * {@link SubjectSaveRequest}：启停是<b>独立的高危操作</b>（AC-2，写
 * {@code sys_operation_log} 的 {@code DISABLE}/{@code ENABLE}），
 * 若它与科目编辑共用一个请求体，日志里就分不清"改了简介"和"下架了科目"。
 *
 * <p>取值只允许 {@code ENABLED}/{@code DISABLED}，由服务层白名单校验——
 * 不做成枚举是因为它出现在 HTTP 边界上，枚举化只会多一次转换。
 */
public record SubjectStatusRequest(

        @NotBlank(message = "状态不能为空")
        String status) {
}
