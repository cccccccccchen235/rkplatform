package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.SysUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户查询与写入。
 *
 * <p>SQL 一律在 {@code resources/mapper/SysUserMapper.xml}，列名显式写出，理由见
 * {@link SubjectMapper} 的说明。
 */
@Mapper
public interface SysUserMapper {

    /** 按登录名查（唯一键 {@code uk_username}）。不存在返回 {@code null}。 */
    SysUser selectByUsername(@Param("username") String username);

    /** 按主键查。不存在返回 {@code null}。 */
    SysUser selectById(@Param("id") Long id);

    /**
     * 插入新用户。
     *
     * <p>回填自增主键到 {@code user.id}（XML 里配 {@code useGeneratedKeys}），
     * 调用方插入后立刻要拿 id 去回读整行。
     *
     * <p>并发插入同一用户名时唯一键会抛 {@code DuplicateKeyException}，
     * 由 rk-service 翻译成 2003——数据库是唯一裁决者，先查再插只是"提前给出更快的答案"。
     */
    int insert(SysUser user);

    /** 更新最近登录时间。返回影响行数，0 表示该用户已不存在。 */
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    // ------------------------------------------------------------------
    // 个人中心（05 §5.3 第 5~7 条）。三条都只动"这一个用户自己的一列"，
    // 且 WHERE 一律带 id——AC-3 要求访问用户数据必须携带 user_id，
    // 服务层传进来的 id 来自会话而不是请求参数（见 UserController）。
    // ------------------------------------------------------------------

    /** 改昵称（05 §5.3 第 5 条）。 */
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /**
     * 改密码（05 §5.3 第 6 条）。
     *
     * <p>同时把 {@code must_change_password} 置 0：06 §6.5 规定"用户改密成功后置 false"，
     * 否则管理员重置过密码的账号会永远停在"必须改密"状态，每次登录都被前端拦回改密页。
     * 两列在同一条 UPDATE 里，不存在"改了密码但没清标记"的中间态。
     */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 设置当前备考科目（05 §5.3 第 7 条）。 */
    int updateCurrentSubjectId(@Param("id") Long id, @Param("currentSubjectId") Long currentSubjectId);
}
