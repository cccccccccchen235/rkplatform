package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.KnowledgePoint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识点标签读写（05 §5.10 第 46~50 条）。
 *
 * <p>与题目的多对多在 {@code rk_question_kp}（04 §4.5），本 Mapper 同时负责
 * 那张关联表上的两个操作：引用计数与合并迁移。
 *
 * <p>SQL 在 {@code resources/mapper/KnowledgePointMapper.xml}。
 */
@Mapper
public interface KnowledgePointMapper {

    /**
     * 按科目取标签。
     *
     * @param includeDisabled 管理端 true（05 §5.10 第 46 条要求含停用项），
     *                        考生端 false（05 §5.2 {@code KnowledgePointVO} 的说明）
     */
    List<KnowledgePoint> selectBySubject(@Param("subjectId") Long subjectId,
                                         @Param("includeDisabled") boolean includeDisabled);

    /** 按主键查。不存在返回 {@code null}。 */
    KnowledgePoint selectById(@Param("id") Long id);

    /**
     * 按科目 + 名称精确查，用于插入前的重名判断。
     *
     * <p>库上的 {@code uk_subject_name} 才是唯一裁决者，这里只是"提前给出更快的答案"；
     * 并发插入仍会抛 {@code DuplicateKeyException}，由服务层翻译成 1001。
     */
    KnowledgePoint selectBySubjectAndName(@Param("subjectId") Long subjectId,
                                          @Param("name") String name);

    int insert(KnowledgePoint knowledgePoint);

    /** 更新可编辑列：{@code name}、{@code aliases}。不含 {@code subject_id}（标签不跨科目搬家）。 */
    int update(KnowledgePoint knowledgePoint);

    int deleteById(@Param("id") Long id);

    /** 引用本标签的题目关联数（{@code rk_question_kp}），删除前检查用。 */
    int countQuestionRefs(@Param("kpId") Long kpId);

    /**
     * 把 {@code sourceId} 上的题目关联迁移到 {@code targetId}，返回新增的关联行数。
     *
     * <p>用 {@code INSERT IGNORE ... SELECT} 而不是 {@code UPDATE rk_question_kp SET kp_id}：
     * 一道题可能<b>同时</b>打了源标签与目标标签（这正是"合并"最常见的场景），
     * {@code UPDATE} 会撞上 {@code uk_question_kp} 的唯一键而整体失败。
     * {@code INSERT IGNORE} 跳过重复行，随后由调用方删除源行——净效果正是"并集"。
     *
     * <p>调用方负责在同一事务里清掉 {@code sourceId} 的残留行。
     */
    int migrateQuestionRefs(@Param("sourceId") Long sourceId,
                            @Param("targetId") Long targetId);

    /**
     * 清掉某标签的全部题目关联行，返回删除行数。
     *
     * <p>合并流程的最后一步：迁移完成后，源标签上剩下的关联行会指向一个已被删除的标签。
     * {@code rk_question_kp} 在 DDL 里<b>没有外键约束</b>，不清理就会留下永久孤儿行，
     * 而它们的表现是"某个统计数字对不上"，极难回溯到合并操作。
     */
    int deleteQuestionRefsByKp(@Param("kpId") Long kpId);

    /**
     * 按 {@code rk_question_kp} 重算某标签的 {@code question_count}（只计 PUBLISHED 题）。
     *
     * <p>合并后必须重算而不是 {@code count = count_source + count_target}：
     * 两标签的交集题会被加两次。冗余列的口径（04 §4.5）是"该标签下的已发布题量"，
     * 只有从关联表重新数一遍才是这个口径。
     */
    int refreshQuestionCount(@Param("kpId") Long kpId);
}
