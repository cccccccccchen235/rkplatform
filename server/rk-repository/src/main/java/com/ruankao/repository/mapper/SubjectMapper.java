package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.Subject;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 科目查询。
 *
 * <p>注册由 {@link com.ruankao.repository.config.MapperScanConfig} 负责
 * （持久层自己声明扫描，不把包名字符串写进启动模块）；{@code @Mapper} 注解保留在这里，
 * 便于 IDE 与静态检查识别。
 *
 * <p>SQL 一律写在同名 XML 中（{@code resources/mapper/SubjectMapper.xml}）：
 * 实体在 rk-domain 且不得带 MyBatis 注解，注解式 SQL 的返回值映射会很难读。
 */
@Mapper
public interface SubjectMapper {

    /**
     * 查询启用中的科目。
     *
     * @param level          级别过滤，null/空表示不过滤
     * @param onlyConfigured true 时仅返回已挂载启用状态卷面模板的科目
     */
    List<Subject> selectSubjects(@Param("level") String level,
                                 @Param("onlyConfigured") boolean onlyConfigured);

    // ------------------------------------------------------------------
    // 以下为管理端与详情接口所用（05 §5.10 第 37~40 条、§5.4 第 9 条）。
    // 与上面那个"考生端列表"方法的关键差别：**这批方法不过滤 status**，
    // 已停用科目必须能查到——管理端要能在列表里看到它并重新启用，
    // 详情接口要能把它判成 3001「科目不存在或已停用」（05 §5.1.2 把
    // "不存在"与"已停用"合并成同一个码，所以这里不需要把两者分开）。
    // ------------------------------------------------------------------

    /**
     * 按主键查，<b>含已停用</b>。不存在返回 {@code null}。
     *
     * <p>过滤 {@code status} 的责任在调用方：详情接口据此把"查不到"与"已停用"
     * 统一判成 3001，而管理端需要拿到已停用的行。
     */
    Subject selectById(@Param("id") Long id);

    /** 管理端科目列表（05 §5.10 第 37 条）：含 {@code DISABLED}，按 level + sort_no 排序。 */
    List<Subject> selectAllForAdmin();

    /** 插入；自增主键回填到 {@code subject.id}。 */
    int insert(Subject subject);

    /**
     * 更新可编辑列（05 §5.10 第 39 条）。
     *
     * <p><b>不含 {@code level}</b>：级别决定卷面类型，改它会架空已配好的模板与已生成的试卷
     * （05 §5.10 第 38 条的说明）。<b>不含 {@code status}</b>：启停是独立的高危操作，
     * 走 {@link #updateStatus} 以便各自写一条审计记录。
     */
    int update(Subject subject);

    /** 启停（05 §5.10 第 40 条）。 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计占用了某科目代码的行数，用于插入/更新前的重名判断。
     *
     * @param excludeId 更新时要排除自己；插入时传 {@code null}
     */
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * 取某级别下当前最大 {@code sort_no}，无记录时返回 0。
     *
     * <p>新增科目的排序号取 {@code max + 1}，让它排在同级末尾。
     * 不做成"插入后重排全部"——一次新增就要动同级别所有行，代价与收益不成比例。
     */
    int selectMaxSortNo(@Param("level") String level);
}
