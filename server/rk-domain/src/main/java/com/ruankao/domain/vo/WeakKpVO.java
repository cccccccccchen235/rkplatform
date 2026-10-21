package com.ruankao.domain.vo;

import java.math.BigDecimal;

/**
 * 薄弱知识点条目（05 §5.8 第 35 条）。按 {@code mastery} 升序，最多 10 条。
 *
 * <p>{@code kpId} 是字符串（ID 类型约定，05 §5.1）；前端拿它跳
 * {@code /practice/KP?refId=...} 去专练这个知识点。
 */
public record WeakKpVO(
        String kpId,
        String kpName,
        Integer answeredCount,
        BigDecimal accuracy,
        BigDecimal mastery) {
}
