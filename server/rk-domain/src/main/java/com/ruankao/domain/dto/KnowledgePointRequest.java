package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识点新增/修改（05 §5.10 第 47/48 条）。
 *
 * <p>{@code aliases} 与库里一致，是<b>一个逗号分隔的字符串</b>而非数组（05 §5.2
 * {@code KnowledgePointVO} 已定此口径）。DTO 与 VO 用同一种表示，不做"数组进、
 * 字符串存"的转换——那会让"存进去的到底是什么"多一层需要推理的东西。
 *
 * <p>唯一性（同科目内 {@code name} 不重复）由库的 {@code uk_subject_name} 兜底，
 * 但服务层必须先查一次并返回 {@code 1001} 而不是让 {@code DuplicateKeyException}
 * 冒到兜底分支变成 {@code 1000}：重名是用户的输入错误，不是系统故障。
 */
public record KnowledgePointRequest(

        @NotBlank(message = "科目 ID 不能为空")
        String subjectId,

        @NotBlank(message = "知识点名称不能为空")
        @Size(max = 64, message = "知识点名称过长")
        String name,

        @Size(max = 255, message = "同义词过长")
        String aliases) {
}
