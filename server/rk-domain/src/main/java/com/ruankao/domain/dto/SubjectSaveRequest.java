package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 科目新增/修改（05 §5.10 第 38/39 条）。
 *
 * <p>字段与 {@link com.ruankao.domain.vo.SubjectVO} 同源，但不是同一个类型：
 * VO 带 {@code id} 与 {@code questionCount} 这类**服务端产出**的字段，
 * 请求体不该有它们——用 DTO 复用 VO 会让"客户端能提交 id"变成一句编译期成立的话。
 *
 * <p><b>{@code level} 在修改时不被采纳</b>（05 §5.10 第 39 条的说明）：科目级别决定
 * {@code paperType} 与卷面结构，改它会架空已经配好的模板与已生成的试卷。
 * 服务层忽略它而不是报错，因为管理端的编辑表单本来就会把只读字段一起提交上来。
 *
 * <p>枚举取值（{@code level}/{@code paperType}/{@code examPeriod}/{@code outlineStatus}/
 * {@code contentStatus}）只在这里判"非空"，**是否落在枚举内由服务层判定**：
 * 那需要 02 文档的取值表，而 DTO 只该表达"这个字段必填、多长"。
 */
public record SubjectSaveRequest(

        @NotBlank(message = "科目级别不能为空")
        String level,

        @NotBlank(message = "科目代码不能为空")
        @Size(max = 32, message = "科目代码过长")
        String code,

        @NotBlank(message = "科目名不能为空")
        @Size(max = 64, message = "科目名过长")
        String name,

        @NotBlank(message = "卷面类型不能为空")
        String paperType,

        @NotBlank(message = "考试期不能为空")
        String examPeriod,

        @NotBlank(message = "大纲状态不能为空")
        String outlineStatus,

        @NotBlank(message = "内容状态不能为空")
        String contentStatus,

        @Size(max = 2000, message = "简介过长")
        String intro) {
}
