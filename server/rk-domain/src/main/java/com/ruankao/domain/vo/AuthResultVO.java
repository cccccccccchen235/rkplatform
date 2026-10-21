package com.ruankao.domain.vo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 注册与登录的统一响应体（05 §5.3 第 1、2 条的 {@code data}）。
 *
 * <p>两条接口的 {@code data} 结构完全相同，故共用一个 VO，而不是各起一个名字
 * （只差名字的两个结构，前端要么写两遍类型，要么写一个联合类型）。
 *
 * <p>{@code expireAt} 是令牌的绝对过期时刻，不是剩余秒数：前端只需要"什么时候该重新登录"，
 * 剩余秒数每多一次网络往返就多一份误差。
 */
public record AuthResultVO(String token, LocalDateTime expireAt, UserVO user) {

    public AuthResultVO {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(user, "user");
    }
}
