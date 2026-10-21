package com.ruankao.service.question.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.PaperTemplateSaveRequest;
import com.ruankao.domain.entity.PaperSection;
import com.ruankao.domain.entity.PaperSitting;
import com.ruankao.domain.entity.PaperTemplate;
import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.PaperTemplateDetailVO;
import com.ruankao.domain.vo.PaperTemplateSaveResultVO;
import com.ruankao.domain.vo.PaperTemplateSaveResultVO.InsufficientQuestionVO;
import com.ruankao.repository.cache.CacheDao;
import com.ruankao.repository.cache.CacheKeys;
import com.ruankao.repository.mapper.ExamQuestionMapper;
import com.ruankao.repository.mapper.PaperTemplateMapper;
import com.ruankao.repository.mapper.SubjectMapper;
import com.ruankao.service.audit.OperationLogService;
import com.ruankao.service.question.PaperTemplateAdminService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端卷面模板配置（05 §5.10 第 51/52 条）。
 *
 * <p>五条校验规则（05 §5.10 第 52 条的"校验规则"段）在本类里逐条实现。为什么放在服务层：
 * 它们都需要"把整份提交看成一个整体"（序号在平铺后连续、场次时长与分节上下限自洽、
 * 满分口径与分节分值一致），逐字段的 Bean Validation 表达不了。
 *
 * <p>三处需要说明的口径：
 * <ol>
 *   <li>满分口径：NOT_SUPPORTED 分节不计入任何满分（02 §2.4.4），所以它在求和时按 0 计；
 *       但时长口径相反，NOT_SUPPORTED 分节的 durationMin/durationMax 照常参与场次时长自洽校验
 *       （种子数据里 IPT 的「应用技术」就是时长 120~150、分值 0，见 04 §4.8.2）。</li>
 *   <li>满分是比对而不是重算：提交值与按分节算出的值不等时返回 1001，而不是静默改写——
 *       10 §10.7 的第一条验收项要求"不符时保存失败"。</li>
 *   <li>题量警告只对 SINGLE_CHOICE 且 SUPPORTED 的分节做；NOT_SUPPORTED 分节不参与组卷，
 *       为它报警告只会制造噪音。</li>
 * </ol>
 */
@Service
public class PaperTemplateAdminServiceImpl implements PaperTemplateAdminService {

    /** AC-2 审计的 module 值（04 §4.9）：模板属于内容维护，与大纲/知识点同域。 */
    private static final String MODULE_SUBJECT = "SUBJECT";

    private static final String TARGET_TYPE_PAPER_TEMPLATE = "PAPER_TEMPLATE";

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String SUPPORTED = "SUPPORTED";

    private static final String OBJECTIVE = "OBJECTIVE";

    private static final String SINGLE_CHOICE = "SINGLE_CHOICE";

    /** 02 §2.4.2 的取值集合。用 List 保持错误消息里的顺序稳定（理由同 AdminSubjectServiceImpl）。 */
    private static final List<String> PAPER_TYPES = List.of("JUNIOR_MID", "SENIOR");

    private static final List<String> SECTION_TYPES = List.of("OBJECTIVE", "SUBJECTIVE", "ESSAY");

    private static final List<String> QUESTION_TYPES =
            List.of("SINGLE_CHOICE", "MULTI_CHOICE", "TRUE_FALSE", "SUBJECTIVE", "ESSAY");

    private static final List<String> SUPPORT_STATUSES = List.of(SUPPORTED, "NOT_SUPPORTED");

    private final SubjectMapper subjectMapper;
    private final PaperTemplateMapper paperTemplateMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final CacheDao cacheDao;
    private final OperationLogService operationLogService;

    public PaperTemplateAdminServiceImpl(SubjectMapper subjectMapper,
                                         PaperTemplateMapper paperTemplateMapper,
                                         ExamQuestionMapper examQuestionMapper,
                                         CacheDao cacheDao,
                                         OperationLogService operationLogService) {
        this.subjectMapper = subjectMapper;
        this.paperTemplateMapper = paperTemplateMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.cacheDao = cacheDao;
        this.operationLogService = operationLogService;
    }

    @Override
    public PaperTemplateDetailVO detail(Long subjectId) {
        requireSubject(subjectId);

        PaperTemplate template = paperTemplateMapper.selectBySubjectId(subjectId);
        if (template == null) {
            throw new BizException(ErrorCode.PAPER_TEMPLATE_MISSING);
        }
        return toDetail(subjectId, template,
                paperTemplateMapper.selectSittings(template.getId()),
                paperTemplateMapper.selectSections(template.getId()));
    }

    @Override
    @Transactional
    public PaperTemplateSaveResultVO save(Long subjectId,
                                          PaperTemplateSaveRequest request,
                                          Long operatorId,
                                          String ip) {
        Subject subject = requireSubject(subjectId);

        validateEnums(request);

        // 校验的同时算满分与题量警告：两件事都要把分节逐条走一遍，
        // 分两轮会让"求和漏掉一个分节"这种错误有机会溜进其中一轮。
        List<InsufficientQuestionVO> warnings = new ArrayList<>();
        int availableSingleChoice = examQuestionMapper.countPublished(subjectId, SINGLE_CHOICE);
        BigDecimal templateTotal = BigDecimal.ZERO;
        BigDecimal templateAuto = BigDecimal.ZERO;

        int expectedSittingNo = 1;
        int expectedSectionNo = 1;
        for (PaperTemplateSaveRequest.Sitting sitting : request.sittings()) {
            requireEqual("场次序号必须从 1 连续递增", expectedSittingNo, sitting.sittingNo());
            expectedSittingNo++;

            BigDecimal sittingTotal = BigDecimal.ZERO;
            BigDecimal sittingAuto = BigDecimal.ZERO;
            int sumDurationMin = 0;
            int sumDurationMax = 0;

            for (PaperTemplateSaveRequest.Section section : sitting.sections()) {
                requireEqual("分节序号必须跨场次全局连续递增", expectedSectionNo, section.sectionNo());
                expectedSectionNo++;

                if (section.durationMin() > section.durationMax()) {
                    throw new BizException(ErrorCode.PARAM_INVALID,
                            "分节「" + section.sectionName() + "」的 durationMin 不能大于 durationMax");
                }
                sumDurationMin += section.durationMin();
                sumDurationMax += section.durationMax();

                boolean supported = SUPPORTED.equals(section.supportStatus());
                BigDecimal sectionScore = supported
                        ? section.perQuestionScore().multiply(BigDecimal.valueOf(section.questionCount()))
                        : BigDecimal.ZERO;
                sittingTotal = sittingTotal.add(sectionScore);
                if (supported && OBJECTIVE.equals(section.sectionType())) {
                    sittingAuto = sittingAuto.add(sectionScore);
                }

                // 规则 6（05 §5.10）：题量不足只警告，不阻断保存。
                if (supported
                        && SINGLE_CHOICE.equals(section.questionType())
                        && section.questionCount() > availableSingleChoice) {
                    warnings.add(new InsufficientQuestionVO(
                            section.sectionNo(),
                            section.sectionName(),
                            section.questionCount(),
                            availableSingleChoice));
                }
            }

            // 规则 3（05 §5.10，对应评审 A6）：场次内总时长共享，分节只约束最早/最晚离开。
            if (sumDurationMin > sitting.totalDuration() || sitting.totalDuration() > sumDurationMax) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "场次 " + sitting.sittingNo() + " 的时长不自洽：需满足 Σ(durationMin) ≤ totalDuration ≤ Σ(durationMax)，"
                                + "当前 " + sumDurationMin + " ≤ " + sitting.totalDuration() + " ≤ " + sumDurationMax + " 不成立");
            }

            // 规则 4（05 §5.10）与"场次满分 = 其分节分值之和"（规则 5 的场次侧）。
            requireScoreEqual("场次 " + sitting.sittingNo() + " 的 totalMaxScore", sitting.totalMaxScore(), sittingTotal);
            requireScoreEqual("场次 " + sitting.sittingNo() + " 的 autoScoredMaxScore", sitting.autoScoredMaxScore(), sittingAuto);

            templateTotal = templateTotal.add(sittingTotal);
            templateAuto = templateAuto.add(sittingAuto);
        }

        // 规则 5（05 §5.10）：模板级满分 = Σ 各场次。
        requireScoreEqual("模板的 totalMaxScore", request.totalMaxScore(), templateTotal);
        requireScoreEqual("模板的 autoScoredMaxScore", request.autoScoredMaxScore(), templateAuto);

        Long templateId = persist(subjectId, request);

        // 科目列表/详情的 paperTotalScore 来自模板表，改了满分必须让缓存失效，
        // 否则考生端最长一小时看不到新的"卷面满分"。
        cacheDao.evict(CacheKeys.SUBJECT_LIST);
        operationLogService.record(operatorId, MODULE_SUBJECT, "UPDATE", TARGET_TYPE_PAPER_TEMPLATE,
                templateId,
                "更新科目 " + subjectId + " (" + subject.getCode() + ") 的卷面模板："
                        + request.sittings().size() + " 个场次，卷面满分 " + request.totalMaxScore(),
                ip);

        return new PaperTemplateSaveResultVO(warnings);
    }

    /** 覆盖式写入：模板 upsert + 子行全删全插。由调用方的事务保证原子性。 */
    private Long persist(Long subjectId, PaperTemplateSaveRequest request) {
        PaperTemplate existing = paperTemplateMapper.selectBySubjectId(subjectId);
        PaperTemplate template = existing == null ? new PaperTemplate() : existing;
        template.setSubjectId(subjectId);
        template.setName(request.name().trim());
        template.setPaperType(request.paperType());
        template.setTotalMaxScore(request.totalMaxScore());
        template.setAutoScoredMaxScore(request.autoScoredMaxScore());
        template.setStatus(STATUS_ENABLED);

        if (existing == null) {
            paperTemplateMapper.insertTemplate(template);
        } else {
            paperTemplateMapper.updateTemplate(template);
        }

        paperTemplateMapper.deleteSections(template.getId());
        paperTemplateMapper.deleteSittings(template.getId());

        for (PaperTemplateSaveRequest.Sitting sitting : request.sittings()) {
            PaperSitting entity = new PaperSitting();
            entity.setTemplateId(template.getId());
            entity.setSittingNo(sitting.sittingNo());
            entity.setSittingName(sitting.sittingName().trim());
            entity.setTotalDuration(sitting.totalDuration());
            entity.setTotalMaxScore(sitting.totalMaxScore());
            entity.setAutoScoredMaxScore(sitting.autoScoredMaxScore());
            entity.setSortNo(sitting.sortNo());
            paperTemplateMapper.insertSitting(entity);

            for (PaperTemplateSaveRequest.Section section : sitting.sections()) {
                PaperSection row = new PaperSection();
                row.setTemplateId(template.getId());
                // sitting_no 由嵌套位置决定，不取请求里的字段（请求里根本没有它）。
                row.setSittingNo(sitting.sittingNo());
                row.setSectionNo(section.sectionNo());
                row.setSectionName(section.sectionName().trim());
                row.setSectionType(section.sectionType());
                row.setQuestionType(section.questionType());
                row.setQuestionCount(section.questionCount());
                row.setPerQuestionScore(section.perQuestionScore());
                row.setDurationMin(section.durationMin());
                row.setDurationMax(section.durationMax());
                row.setAllowEarlySubmit(section.allowEarlySubmit());
                row.setSupportStatus(section.supportStatus());
                row.setSortNo(section.sortNo());
                paperTemplateMapper.insertSection(row);
            }
        }
        return template.getId();
    }

    private PaperTemplateDetailVO toDetail(Long subjectId,
                                           PaperTemplate template,
                                           List<PaperSitting> sittings,
                                           List<PaperSection> sections) {
        Map<Integer, List<PaperTemplateDetailVO.Section>> bySitting = new LinkedHashMap<>();
        for (PaperSection section : sections) {
            bySitting.computeIfAbsent(section.getSittingNo(), key -> new ArrayList<>())
                    .add(new PaperTemplateDetailVO.Section(
                            section.getSectionNo(),
                            section.getSectionName(),
                            section.getSectionType(),
                            section.getQuestionType(),
                            section.getQuestionCount(),
                            section.getPerQuestionScore(),
                            section.getDurationMin(),
                            section.getDurationMax(),
                            section.getAllowEarlySubmit(),
                            section.getSupportStatus(),
                            section.getSortNo()));
        }

        List<PaperTemplateDetailVO.Sitting> sittingVOs = new ArrayList<>(sittings.size());
        int totalDuration = 0;
        for (PaperSitting sitting : sittings) {
            totalDuration += sitting.getTotalDuration() == null ? 0 : sitting.getTotalDuration();
            sittingVOs.add(new PaperTemplateDetailVO.Sitting(
                    sitting.getSittingNo(),
                    sitting.getSittingName(),
                    sitting.getTotalDuration(),
                    sitting.getTotalMaxScore(),
                    sitting.getAutoScoredMaxScore(),
                    sitting.getSortNo(),
                    bySitting.getOrDefault(sitting.getSittingNo(), List.of())));
        }

        return new PaperTemplateDetailVO(
                subjectId,
                template.getName(),
                template.getPaperType(),
                totalDuration,
                template.getTotalMaxScore(),
                template.getAutoScoredMaxScore(),
                sittingVOs);
    }

    private Subject requireSubject(Long subjectId) {
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }
        return subject;
    }

    private static void validateEnums(PaperTemplateSaveRequest request) {
        requireIn("paperType", request.paperType(), PAPER_TYPES);
        for (PaperTemplateSaveRequest.Sitting sitting : request.sittings()) {
            for (PaperTemplateSaveRequest.Section section : sitting.sections()) {
                requireIn("sectionType", section.sectionType(), SECTION_TYPES);
                requireIn("questionType", section.questionType(), QUESTION_TYPES);
                requireIn("supportStatus", section.supportStatus(), SUPPORT_STATUSES);
            }
        }
    }

    private static void requireIn(String field, String value, List<String> allowed) {
        if (!allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    field + " 取值非法：" + value + "（允许：" + String.join("/", allowed) + "）");
        }
    }

    private static void requireEqual(String message, int expected, Integer actual) {
        if (actual == null || actual != expected) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    message + "：期望 " + expected + "，实际 " + actual);
        }
    }

    /**
     * 满分口径比对。用 compareTo 而不是 equals：BigDecimal 的 equals 连小数位也算，
     * 150 与 150.0 会被判为不等，而 perQuestionScore 是 DECIMAL(4,1)，
     * 乘出来的结果天然带小数位——它们在分值语义上是同一个数。
     */
    private static void requireScoreEqual(String label, Integer submitted, BigDecimal computed) {
        if (submitted == null || computed.compareTo(BigDecimal.valueOf(submitted)) != 0) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    label + "与分节分值之和不符：按分节计算应为 " + computed.stripTrailingZeros().toPlainString()
                            + "，实际提交 " + submitted);
        }
    }
}
