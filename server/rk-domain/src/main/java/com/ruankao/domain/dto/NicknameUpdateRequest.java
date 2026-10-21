package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改昵称（05 §5.3 第 5 条）。
 *
 * <p>字段名与 05 §5.3 第 4 条的 {@code UserVO.nickname} 同名，不是新造的词。
 * 05 只给了路径与方法、没给请求体，本类据此定形并登记在 {@code docs/33}。
 *
 * <p>长度上限 32 与注册时的昵称一致（05 §5.3 第 1 条）：同一条数据在两个入口
 * 必须受同一约束，否则会出现"注册时改不了、注册后能改成 64 字"的自相矛盾。
 */
public record NicknameUpdateRequest(

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称最长 32 位")
        String nickname) {
}
