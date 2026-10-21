package com.ruankao.service.user;

import com.ruankao.domain.entity.SysUser;
import com.ruankao.domain.vo.UserVO;

/**
 * 用户实体 → VO（03 §3.3.2：每个业务包内配一个 Converter）。
 *
 * <p>这里是 {@code passwordHash} 的最后一站：它<b>不出现在</b> {@link UserVO} 的任何字段上，
 * 所以从这一层往上（Service 返回值、Controller、响应体、日志）都不可能出现口令
 * （03 §3.8 AC-3）。要新增用户相关的 VO 时，请不要图省事直接返回实体。
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(SysUser entity) {
        return new UserVO(
                // 主键是 BIGINT UNSIGNED，转字符串下发以避免前端 JS 丢精度（05 §5.1）
                String.valueOf(entity.getId()),
                entity.getUsername(),
                entity.getNickname(),
                entity.getRole(),
                entity.getStatus(),
                entity.getCurrentSubjectId() == null ? null : String.valueOf(entity.getCurrentSubjectId()),
                Boolean.TRUE.equals(entity.getMustChangePassword()),
                entity.getLastLoginAt(),
                entity.getCreatedAt());
    }
}
