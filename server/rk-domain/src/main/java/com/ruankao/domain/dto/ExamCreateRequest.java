package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;

/** 发起模拟考（05 §5.6 第 16 条）。 */
public class ExamCreateRequest {

    @NotBlank(message = "科目 ID 不能为空")
    private String subjectId;

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
}
