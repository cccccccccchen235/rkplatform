package com.ruankao.service.question.impl;

import cn.hutool.core.lang.TypeReference;
import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.entity.PaperTemplate;
import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.enums.SubjectLevel;
import com.ruankao.domain.vo.PaperSummaryVO;
import com.ruankao.domain.vo.SubjectVO;
import com.ruankao.repository.cache.CacheDao;
import com.ruankao.repository.cache.CacheKeys;
import com.ruankao.repository.mapper.PaperTemplateMapper;
import com.ruankao.repository.mapper.SubjectMapper;
import com.ruankao.service.question.PaperSummaryConverter;
import com.ruankao.service.question.SubjectConverter;
import com.ruankao.service.question.SubjectService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SubjectServiceImpl implements SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectServiceImpl.class);

    /**
     * 缓存的载荷类型。
     *
     * <p>必须是 {@code TypeReference} 而不是 {@code Map.class}：后者会让嵌套的
     * {@code List<SubjectVO>} 在反序列化时退化成 {@code List<JSONObject>}，
     * 而这个错误<b>不会在缓存写入时报</b>——它只在命中缓存的那次请求上炸，
     * 于是表现为"第一次访问正常，一小时内第二次访问 500"。
     */
    private static final TypeReference<Map<String, List<SubjectVO>>> SUBJECT_LIST_TYPE =
            new TypeReference<>() {
            };

    private final SubjectMapper subjectMapper;
    private final PaperTemplateMapper paperTemplateMapper;
    private final CacheDao cacheDao;

    public SubjectServiceImpl(SubjectMapper subjectMapper,
                              PaperTemplateMapper paperTemplateMapper,
                              CacheDao cacheDao) {
        this.subjectMapper = subjectMapper;
        this.paperTemplateMapper = paperTemplateMapper;
        this.cacheDao = cacheDao;
    }

    @Override
    public Map<String, List<SubjectVO>> listGroupedByLevel(String level, boolean onlyConfigured) {
        Map<String, List<SubjectVO>> all = cacheDao.getOrLoad(
                CacheKeys.SUBJECT_LIST,
                SUBJECT_LIST_TYPE,
                CacheDao.DEFAULT_TTL,
                this::loadAllGrouped);
        return applyFilters(all, level, onlyConfigured);
    }

    @Override
    public SubjectVO getDetail(Long subjectId) {
        Subject subject = subjectMapper.selectById(subjectId);
        // "不存在"与"已停用"合并成 3001（05 §5.1.2 就是这么定的）：
        // 对考生而言两者的结论相同（这个科目不能考），分开反而泄漏了
        // "这个 ID 在库里存在但被下架了"这一信息。
        if (subject == null || !"ENABLED".equals(subject.getStatus())) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }
        return SubjectConverter.toVO(subject, loadPaperSummary(subjectId));
    }

    /**
     * 回源：不带任何过滤条件的全量分组结果。只有缓存未命中时才会走到。
     */
    private Map<String, List<SubjectVO>> loadAllGrouped() {
        List<Subject> subjects = subjectMapper.selectSubjects(null, false);

        // 先按枚举声明序建好三个空桶，保证空组也出现在响应里（05 §5.4）
        Map<String, List<SubjectVO>> grouped = new LinkedHashMap<>();
        for (SubjectLevel subjectLevel : SubjectLevel.values()) {
            grouped.put(subjectLevel.name(), new ArrayList<>());
        }

        for (Subject subject : subjects) {
            List<SubjectVO> bucket = grouped.get(subject.getLevel());
            if (bucket == null) {
                // 库里的 level 落在枚举之外。丢一行比整页 500 好，但必须留下痕迹：
                // 这只可能来自人工改库或数据导入错误，静默忽略会让该科目"凭空消失"。
                log.warn("科目 level 不在 SubjectLevel 枚举内，已跳过：id={}, code={}, level={}",
                        subject.getId(), subject.getCode(), subject.getLevel());
                continue;
            }
            // 列表接口不带卷面摘要树（05 §5.4 第 9 条：paperSummary 属于详情接口），
            // 但带一个整数 paperTotalScore（来自实体的关联投影列），供列表页判断卷面规模。
            bucket.add(SubjectConverter.toVO(subject, null));
        }

        Map<String, List<SubjectVO>> result = new LinkedHashMap<>();
        grouped.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return result;
    }

    /**
     * 在内存里套用两个过滤条件。
     *
     * <p>{@code onlyConfigured} 用 {@code paperTotalScore != null} 判断，
     * 而不是再查一次库：那个投影列的口径就是"存在启用中的模板"
     * （{@code SubjectMapper} 的 SQL 注释已声明两者条件逐字一致）。
     */
    private static Map<String, List<SubjectVO>> applyFilters(Map<String, List<SubjectVO>> all,
                                                             String level,
                                                             boolean onlyConfigured) {
        String normalized = normalizeLevel(level);
        Map<String, List<SubjectVO>> result = new LinkedHashMap<>(all.size());
        all.forEach((key, value) -> {
            List<SubjectVO> bucket = (normalized == null || normalized.equals(key))
                    ? value
                    : List.of();
            if (onlyConfigured) {
                bucket = bucket.stream()
                        .filter(vo -> vo.paperTotalScore() != null)
                        .toList();
            }
            result.put(key, bucket);
        });
        return result;
    }

    /**
     * 装配卷面摘要。没有启用中的模板时返回 {@code null}
     * （{@code SubjectVO.paperSummary} 与 {@code paperTotalScore} 的口径一致）。
     */
    private PaperSummaryVO loadPaperSummary(Long subjectId) {
        PaperTemplate template = paperTemplateMapper.selectEnabledBySubjectId(subjectId);
        if (template == null) {
            return null;
        }
        return PaperSummaryConverter.toVO(
                template,
                paperTemplateMapper.selectSittings(template.getId()),
                paperTemplateMapper.selectSections(template.getId()));
    }

    /**
     * 归一化级别过滤值：去空格、转大写。非法取值不报 1001——它只是一个过滤条件，
     * 查不到就是空列表，为它打断用户的列表页不值得（05 §5.4 未定义该错误码）。
     */
    private static String normalizeLevel(String level) {
        if (level == null) {
            return null;
        }
        String trimmed = level.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }
}
