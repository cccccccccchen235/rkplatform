package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 收藏/取消收藏请求（05 §5.7 第 29 条）。
 *
 * <p>用可变类而非 record：与其它写入型请求保持一致的风格，后续若增加备注等字段
 * 也不触发"全字段构造"签名变更。
 *
 * <p><b>刻意不收 {@code subjectId}</b>：它由服务端从 {@code rk_question} 反查得到，
 * 避免客户端把收藏挂到错误的科目上——一旦客户端传错，按科目筛选收藏/统计的口径
 * 就会静默错位，而这种错误在单科目测试里不会暴露。
 */
@Getter
@Setter
@NoArgsConstructor
public class FavoriteRequest {

    @NotNull(message = "题目 ID 不能为空")
    private String questionId;
}