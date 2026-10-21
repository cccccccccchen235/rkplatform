package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.OutlineNode;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 大纲章节节点读写（05 §5.10 第 41~45 条）。
 *
 * <p>章节树只有 3 层、单科目节点数在几十的量级（04 §4.5），因此
 * <b>不做递归 SQL、不做父子分页</b>：一次取全科目节点，在 Service 里组装成树。
 * 递归 CTE 在 MySQL 8 上可行，但树形接口每次都要交付整棵树，
 * 一次全取比"N 次递归查询"少 N-1 次往返，且让"层级由 parentId 推导"的
 * 一致性校验能做在内存里。
 *
 * <p>SQL 在 {@code resources/mapper/OutlineNodeMapper.xml}。
 */
@Mapper
public interface OutlineNodeMapper {

    /**
     * 按科目取全部节点，按 {@code level, sort_no, id} 升序返回（组装树时父节点先于子节点）。
     *
     * @param includeDisabled 管理端为 true（要能看见已停用节点），考生端为 false
     */
    List<OutlineNode> selectBySubject(@Param("subjectId") Long subjectId,
                                      @Param("includeDisabled") boolean includeDisabled);

    /** 按主键查，含已停用。不存在返回 {@code null}。 */
    OutlineNode selectById(@Param("id") Long id);

    /** 插入；自增主键回填到 {@code node.id}（XML 配 {@code useGeneratedKeys}）。 */
    int insert(OutlineNode node);

    /**
     * 更新可编辑列：{@code parent_id}、{@code level}、{@code name}、{@code sort_no}、{@code origin}。
     *
     * <p>{@code level} 由 Service 依 {@code parentId} 推导后传入，不由调用方随意指定——
     * "子节点的 level 必须是父节点 level + 1"这条不变量只有一个推导点，
     * 才不会出现"level=3 却挂在 level=1 下"的行。
     * <b>不含 {@code subject_id}</b>：章节不能跨科目搬家，那会让挂在它下面的题目
     * 指向别科目的章节路径。
     */
    int update(OutlineNode node);

    /**
     * 单节点改父、排序与层级（拖拽排序的最小写）。返回影响行数。
     *
     * <p>带 {@code level} 不是冗余：跨层拖拽（把三级章节拖成一级）必须同时改层级，
     * 否则下一次按 {@code level} 排序取数时，这棵子树会插到错误的位置上去。
     */
    int updateSort(@Param("id") Long id,
                   @Param("parentId") Long parentId,
                   @Param("sortNo") Integer sortNo,
                   @Param("level") Integer level);

    /**
     * 只改层级，用于"父节点层级变了、子孙跟着平移"的级联。
     *
     * <p>与 {@link #updateSort} 分开是因为级联时子孙的 {@code parent_id} 与
     * {@code sort_no} 都没有变——把它们一起写一遍不仅多改两列，
     * 还让"这次只是平移了层级"这件事在 SQL 日志里看不出来。
     */
    int updateLevel(@Param("id") Long id, @Param("level") Integer level);

    int deleteById(@Param("id") Long id);

    /** 直接子节点数；>0 时不允许删除（否则留下无父的孤儿子树）。 */
    int countChildren(@Param("parentId") Long parentId);

    /**
     * 同级当前最大 {@code sort_no}，无同级节点时返回 0；新节点据此排到末尾。
     *
     * @param parentId 父节点；{@code 0} = 根层级
     */
    int selectMaxSortNo(@Param("subjectId") Long subjectId, @Param("parentId") Long parentId);

    /**
     * 引用本章节的题目数，用于删除前的引用检查（05 §5.10 第 44 条：有引用返回 1005）。
     *
     * <p><b>不按 status 过滤</b>：草稿题、待审题同样通过 {@code outline_id} 指向本章节，
     * 删掉章节会让它们的"章节路径"（{@code QuestionVO.outlinePath}）指向不存在的位置。
     * 只统计 PUBLISHED 会漏掉这些，把一次数据损坏放行。
     */
    int countQuestionRefs(@Param("outlineId") Long outlineId);
}
