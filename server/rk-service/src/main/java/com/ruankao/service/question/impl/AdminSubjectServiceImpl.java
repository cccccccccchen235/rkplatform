package com.ruankao.service.question.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.dto.SubjectSaveRequest;
import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.AdminSubjectVO;
import com.ruankao.repository.cache.CacheDao;
import com.ruankao.repository.cache.CacheKeys;
import com.ruankao.repository.mapper.SubjectMapper;
import com.ruankao.service.audit.OperationLogService;
import com.ruankao.service.question.AdminSubjectConverter;
import com.ruankao.service.question.AdminSubjectService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSubjectServiceImpl implements AdminSubjectService {

    /** AC-2 审计的 {@code module} 值（04 §4.9 的取值集合）。 */
    private static final String MODULE_SUBJECT = "SUBJECT";

    private static final String TARGET_TYPE_SUBJECT = "SUBJECT";

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String STATUS_DISABLED = "DISABLED";

    /**
     * 02 §2.2 的取值集合。
     *
     * <p>用 {@code List} 而不是 {@code Set}：取值会拼进错误提示，{@code Set.of} 的
     * 迭代顺序未指定，同一条错误在不同 JVM 上会打印不同的顺序——那会让
     * "照着提示改"的人以为看到了不同的规则。元素只有个位数，{@code contains} 的
     * O(n) 可以忽略。
     */
    private static final List<String> LEVELS = List.of("JUNIOR", "MIDDLE", "SENIOR");

    private static final List<String> PAPER_TYPES = List.of("JUNIOR_MID", "SENIOR");

    private static final List<String> EXAM_PERIODS = List.of("BOTH", "FIRST_HALF", "SECOND_HALF");

    private static final List<String> OUTLINE_STATUSES = List.of("FULL", "SKELETON");

    private static final List<String> CONTENT_STATUSES = List.of("SAMPLE", "BULK");

    private static final List<String> STATUSES = List.of(STATUS_ENABLED, STATUS_DISABLED);

    private final SubjectMapper subjectMapper;
    private final CacheDao cacheDao;
    private final OperationLogService operationLogService;

    public AdminSubjectServiceImpl(SubjectMapper subjectMapper,
                                   CacheDao cacheDao,
                                   OperationLogService operationLogService) {
        this.subjectMapper = subjectMapper;
        this.cacheDao = cacheDao;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<AdminSubjectVO> listForAdmin() {
        return AdminSubjectConverter.toVOList(subjectMapper.selectAllForAdmin());
    }

    @Override
    @Transactional
    public String create(SubjectSaveRequest request, Long operatorId, String ip) {
        validateEnums(request);

        if (subjectMapper.countByCode(request.code().trim(), null) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "科目代码已存在：" + request.code());
        }

        Subject subject = new Subject();
        subject.setLevel(request.level());
        subject.setCode(request.code().trim());
        subject.setName(request.name().trim());
        subject.setPaperType(request.paperType());
        subject.setExamPeriod(request.examPeriod());
        subject.setOutlineStatus(request.outlineStatus());
        subject.setContentStatus(request.contentStatus());
        subject.setIntro(request.intro());
        // 排到同级末尾。同级别并发的两次新增可能取到同一个 max+1，
        // 但排序号重复只是显示顺序不稳定，不是数据损坏——不值得为它加锁。
        subject.setSortNo(subjectMapper.selectMaxSortNo(request.level()) + 1);
        subject.setStatus(STATUS_ENABLED);

        subjectMapper.insert(subject);

        cacheDao.evict(CacheKeys.SUBJECT_LIST);
        operationLogService.record(operatorId, MODULE_SUBJECT, "CREATE", TARGET_TYPE_SUBJECT,
                subject.getId(),
                "新增科目 " + subject.getId() + " (" + subject.getCode() + ") " + subject.getName(),
                ip);

        return String.valueOf(subject.getId());
    }

    @Override
    @Transactional
    public void update(Long subjectId, SubjectSaveRequest request, Long operatorId, String ip) {
        Subject existing = requireExisting(subjectId);
        validateEnums(request);

        if (subjectMapper.countByCode(request.code().trim(), subjectId) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "科目代码已存在：" + request.code());
        }

        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setCode(request.code().trim());
        subject.setName(request.name().trim());
        subject.setPaperType(request.paperType());
        subject.setExamPeriod(request.examPeriod());
        subject.setOutlineStatus(request.outlineStatus());
        subject.setContentStatus(request.contentStatus());
        subject.setIntro(request.intro());
        // 排序号不由本接口修改，但 update 语句会写这一列，所以必须把原值带上——
        // 否则写进去的是 null，而 sort_no 是 NOT NULL，整条 UPDATE 直接失败。
        subject.setSortNo(existing.getSortNo());

        subjectMapper.update(subject);

        cacheDao.evict(CacheKeys.SUBJECT_LIST);
        operationLogService.record(operatorId, MODULE_SUBJECT, "UPDATE", TARGET_TYPE_SUBJECT,
                subjectId,
                "修改科目 " + subjectId + " (" + subject.getCode() + ") " + subject.getName(),
                ip);
    }

    @Override
    @Transactional
    public void changeStatus(Long subjectId, String status, Long operatorId, String ip) {
        Subject existing = requireExisting(subjectId);

        if (!STATUSES.contains(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态取值非法：" + status);
        }
        if (status.equals(existing.getStatus())) {
            // 幂等：状态没变就不动库、不记审计。重复提交（用户连点两次）
            // 不该在审计日志里留下两条一模一样的记录。
            return;
        }

        subjectMapper.updateStatus(subjectId, status);

        cacheDao.evict(CacheKeys.SUBJECT_LIST);
        // 停用 = 下架，是 P2 引入的第一个 AC-2 高危操作：
        // action 用 DISABLE / ENABLE 而不是笼统的 UPDATE，
        // 这样"查所有下架操作"是一条 WHERE action = 'DISABLE' 的查询。
        operationLogService.record(operatorId, MODULE_SUBJECT,
                STATUS_DISABLED.equals(status) ? "DISABLE" : "ENABLE",
                TARGET_TYPE_SUBJECT, subjectId,
                "科目 " + subjectId + " (" + existing.getCode() + ") 状态 "
                        + existing.getStatus() + " → " + status,
                ip);
    }

    /**
     * 取科目，不存在则 3001。
     *
     * <p>这里用 3001 而不是 1003：管理端操作的"不存在"与考生端看到的
     * "不存在或已停用"是同一件事（05 §5.1.2 只有一个 3001），
     * 而且管理端对已停用科目也走这条路径（要能重新启用），口径自然一致。
     */
    private Subject requireExisting(Long subjectId) {
        Subject existing = subjectMapper.selectById(subjectId);
        if (existing == null) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }
        return existing;
    }

    /**
     * 校验枚举列取值。
     *
     * <p>在服务层做而不是靠 Bean Validation 注解：错误码要是 1001 且带上具体字段与取值，
     * 让调用方一眼看出是哪个字段拼错了。放在这里还让"合法取值有哪些"离写入点最近——
     * 将来 02 文档改了取值集合，改这一个方法就够。
     */
    private static void validateEnums(SubjectSaveRequest request) {
        requireIn("level", request.level(), LEVELS);
        requireIn("paperType", request.paperType(), PAPER_TYPES);
        requireIn("examPeriod", request.examPeriod(), EXAM_PERIODS);
        requireIn("outlineStatus", request.outlineStatus(), OUTLINE_STATUSES);
        requireIn("contentStatus", request.contentStatus(), CONTENT_STATUSES);
    }

    private static void requireIn(String field, String value, List<String> allowed) {
        if (!allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    field + " 取值非法：" + value + "（允许：" + String.join("/", allowed) + "）");
        }
    }
}
