package com.ruankao.api.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 取请求来源 IP，供 AC-2 的操作审计使用。
 *
 * <p>取值优先级：{@code X-Real-IP} → {@code X-Forwarded-For} 的第一段 → {@code getRemoteAddr()}。
 *
 * <p><b>为什么 {@code X-Real-IP} 优先</b>：09 §9.x 的 Nginx 配置里，
 * {@code X-Real-IP} 被设为 {@code $remote_addr}——它是 Nginx <b>覆盖写入</b>的，
 * 客户端伪造不了。而 {@code X-Forwarded-For} 用的是 {@code $proxy_add_x_forwarded_for}，
 * 语义是"把上游传来的 XFF 原样保留、再追加真实地址"，于是它的<b>第一段是客户端可伪造的</b>。
 * 两者都能拿到真值时优先取不可伪造的那个。
 *
 * <p><b>已知限制（诚实登记）</b>：应用若被<b>直接</b>访问（绕过 Nginx），这两个头都来自客户端，
 * 审计里的 IP 就只是一个"声称值"。单机部署（09 §9.x）下 Nginx 是唯一入口，
 * 该限制不成立；换拓扑时要重新评估。审计的价值主要在于 user_id 与时间线，
 * IP 是辅助信息，因此不为它引入额外的基础设施。
 *
 * <p>返回长度截断到 45 字符（{@code sys_operation_log.ip} 是 {@code VARCHAR(45)}，
 * 兼容 IPv6）。超长只会出现在伪造的头里——截断而不是丢弃：截断后至少还能看出它在被伪造。
 */
public final class ClientIpResolver {

    /** {@code sys_operation_log.ip} 的列宽（45 = IPv6 文本形式的长度）。 */
    private static final int MAX_LENGTH = 45;

    private static final String UNKNOWN = "unknown";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        if (realIp != null && !UNKNOWN.equalsIgnoreCase(realIp)) {
            return truncate(realIp);
        }

        String forwarded = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            // XFF 是逗号分隔的链：取第一段（原始的客户端地址）
            String first = trimToNull(forwarded.split(",")[0]);
            if (first != null && !UNKNOWN.equalsIgnoreCase(first)) {
                return truncate(first);
            }
        }

        return truncate(trimToNull(request.getRemoteAddr()));
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String ip) {
        if (ip == null || ip.length() <= MAX_LENGTH) {
            return ip;
        }
        return ip.substring(0, MAX_LENGTH);
    }
}
