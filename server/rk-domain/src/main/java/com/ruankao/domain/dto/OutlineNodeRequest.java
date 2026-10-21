package com.ruankao.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 章节新增/修改（05 §5.10 第 42/43 条）。
 *
 * <p>用可变类而非 record：修改接口的路径上 {id} 是权威的，请求体里的 {@code subjectId}
 * 在"改"的语义下应当被忽略。record 的"全字段构造"会让人以为它必须被填。
 *
 * <p><b>{@code parentId} 用 {@code String} 接收</b>：前端 JSON 里 ID 一律是字符串
 * （05 §5.2 所有 ID 都是 string），这里跟着走，避免前端为"根节点填 0"再转一次数字。
 * {@code "0"} 即根。空白字符串按 {@code "0"} 处理——管理端"新增顶级章节"时
 * 下拉框可能提交空值。
 *
 * <p><b>不接收 {@code level}</b>：层级由 {@code parentId} 推导（父级 level + 1），
 * 这是唯一不会出现"level=3 却挂在 level=1 下"的口径。让客户端提交 level 等于
 * 把一条可以在服务端恒定推导的不变量交给客户端维护。
 *
 * @param origin 出处：{@code OFFICIAL_TITLE} 官方标题短语 / {@code MANUAL} 人工新增。
 *               管理端表单不提供该字段，缺省即 {@code MANUAL}；留出口是给
 *               {@code deploy/sql/outline-*.sql} 之外的导入路径用（05 §5.10 第 42 条列了它）。
 *               白名单校验，不允许其它值——否则这个标记就失去了"可筛选"的意义。
 */
@Getter
@Setter
@NoArgsConstructor
public class OutlineNodeRequest {

    @NotNull(message = "科目 ID 不能为空")
    private String subjectId;

    /** 父节点 ID；{@code "0"} 或空/缺省 = 根。 */
    private String parentId;

    @NotBlank(message = "章节名不能为空")
    @Size(max = 128, message = "章节名过长")
    private String name;

    @Min(value = 0, message = "排序号不能为负")
    private Integer sortNo;

    @Pattern(regexp = "OFFICIAL_TITLE|MANUAL", message = "出处取值非法")
    private String origin;
}
