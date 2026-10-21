package com.ruankao.common.api;

/**
 * 字段级校验错误（05 §5.1.2：`1001` 的 {@code data} 为 {@code [{field, message}]}）。
 *
 * @param field   出错字段名，对应 DTO 属性名
 * @param message 面向用户的提示文案
 */
public record FieldErrorVO(String field, String message) {
}
