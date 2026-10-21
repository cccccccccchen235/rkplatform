package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.PaperSection;
import com.ruankao.domain.entity.PaperSitting;
import com.ruankao.domain.entity.PaperTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 卷面模板读写（读取见 05 §5.4 第 9 条的 {@code paperSummary}，读写见 §5.10 第 51/52 条）。
 *
 * <p>三次查询而不是一条三表 JOIN：模板 1 行、场次 ≤2 行、分节 ≤3 行，
 * 全部走 {@code uk_*}/{@code idx_*} 索引。用一条 JOIN 换来的"少两次往返"
 * 抵不过它带来的空值处理（LEFT JOIN 下无分节的场次会出现整行 null），
 * 而这段代码每读一次都要重新推一遍那个 null 分支。
 *
 * <p>写入采用"删子行 + 重插"而不是逐行 diff：模板是整体覆盖式提交（05 §5.10 第 52 条），
 * 一次提交最多 2 + 3 行子数据。逐行 diff 要处理"改了序号 = 删旧建新还是就地改"
 * 这种没有正确答案的歧义，而覆盖式写入的语义与契约一字不差。
 *
 * <p>SQL 在 {@code resources/mapper/PaperTemplateMapper.xml}。
 */
@Mapper
public interface PaperTemplateMapper {

    /**
     * 取某科目启用中的卷面模板；没有模板或模板已停用时返回 {@code null}。
     *
     * <p>按 {@code uk_subject(subject_id)} 至多一行，所以不存在"取哪一行"的歧义。
     * 过滤 {@code status = 'ENABLED'} 与 {@code SubjectMapper.paperTotalScoreColumn}
     * 的标量子查询逐字一致——否则会出现"列表有满分、详情没有模板"的自相矛盾。
     */
    PaperTemplate selectEnabledBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * 管理端用：取某科目的模板，<b>不过滤 {@code status}</b>。
     *
     * <p>与 {@link #selectEnabledBySubjectId} 分开：考生端只看启用中的模板
     * （停用模板对考生等于不存在），而管理端必须能看到并修改任意状态的模板，
     * 否则停用过的模板会在这个页面上"消失"，管理员只会以为数据丢了。
     */
    PaperTemplate selectBySubjectId(@Param("subjectId") Long subjectId);

    /** 取模板的全部场次，按 {@code sitting_no} 升序。 */
    List<PaperSitting> selectSittings(@Param("templateId") Long templateId);

    /** 取模板的全部分节，按 {@code section_no} 升序（全局连续，跨场次不断号）。 */
    List<PaperSection> selectSections(@Param("templateId") Long templateId);

    /** 新增模板；自增主键回填到 {@code template.id}。 */
    int insertTemplate(PaperTemplate template);

    /**
     * 更新模板可编辑列（05 §5.10 第 52 条）。
     *
     * <p>把 {@code status} 一并置回 {@code ENABLED}：本接口是"配置这份卷面"的入口，
     * 配置完却仍是停用状态会让考生端仍然拿不到模板，而 51/52 并没有单独的启停接口。
     */
    int updateTemplate(PaperTemplate template);

    /** 删除模板的全部分节（覆盖式写入的第一步）。 */
    int deleteSections(@Param("templateId") Long templateId);

    /** 删除模板的全部场次（覆盖式写入的第一步）。 */
    int deleteSittings(@Param("templateId") Long templateId);

    /** 新增场次；自增主键回填到 {@code sitting.id}。 */
    int insertSitting(PaperSitting sitting);

    /** 新增分节；自增主键回填到 {@code section.id}。 */
    int insertSection(PaperSection section);
}
