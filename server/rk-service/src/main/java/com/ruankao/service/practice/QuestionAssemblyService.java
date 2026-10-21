package com.ruankao.service.practice;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.entity.QuestionOption;
import com.ruankao.domain.vo.OutlineNodeVO;
import com.ruankao.domain.vo.QuestionDetailVO;
import com.ruankao.domain.vo.QuestionImageVO;
import com.ruankao.domain.vo.QuestionKpRowVO;
import com.ruankao.domain.vo.QuestionKpVO;
import com.ruankao.domain.vo.QuestionOptionVO;
import com.ruankao.domain.vo.QuestionVO;
import com.ruankao.domain.enums.QuestionType;
import com.ruankao.repository.mapper.QuestionMapper;
import com.ruankao.repository.mapper.UserFavoriteMapper;
import com.ruankao.service.question.OutlineService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 题目实体 → 接口 VO 的装配（05 §5.2 的 {@code QuestionVO} / {@code QuestionDetailVO}）。
 *
 * <p>单独成类而不是塞进练习 Service：做题、收藏列表、错题回顾三处都要把"一行题目"
 * 组装成前端认识的形状，且要补四样关联数据（选项、知识点、章节路径、是否已收藏）。
 * 这四样各自都有"查不到就是空"的正常分支，散在三个 Service 里会各写一遍。
 *
 * <p>装配是**批量**的：一次把一页题的选项/知识点/收藏/路径各查一次，
 * 而不是每道题查四次。20 道题的卷子因此是 4 次查询而不是 80 次——
 * 这也是为什么本类的方法签名收 {@code List<Question>} 而不是单个 {@code Question}。
 */
@Service
public class QuestionAssemblyService {

    private static final Logger log = LoggerFactory.getLogger(QuestionAssemblyService.class);

    private final QuestionMapper questionMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final OutlineService outlineService;

    public QuestionAssemblyService(QuestionMapper questionMapper,
                                   UserFavoriteMapper userFavoriteMapper,
                                   OutlineService outlineService) {
        this.questionMapper = questionMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.outlineService = outlineService;
    }

    /**
     * 装配"含答案"的题目（05 §5.2 {@link QuestionDetailVO}）。
     *
     * <p>练习模式下发答案是有意的（05 §5.5 的核心差异表）：练习要"答完即看解析"，
     * 先给答案能省掉每题一次往返；服务端在答题上报时仍会重新判分。
     *
     * <p>{@code isCorrect} / {@code userAnswer} 在这里恒为 {@code null}：
     * 这是"取卷"时刻，用户还没作答（05 §5.2 已声明这两个字段表示"本次作答"）。
     *
     * @param userId 当前登录用户；游客传 {@code null}，此时 {@code isFavorited} 恒为 false
     */
    public List<QuestionDetailVO> toDetailList(Long userId, List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Context ctx = loadContext(userId, questions);
        List<QuestionDetailVO> result = new ArrayList<>(questions.size());
        for (Question q : questions) {
            result.add(new QuestionDetailVO(
                    String.valueOf(q.getId()),
                    String.valueOf(q.getSubjectId()),
                    String.valueOf(q.getOutlineId()),
                    ctx.outlinePaths.getOrDefault(q.getOutlineId(), List.of()),
                    q.getQuestionType(),
                    q.getStem(),
                    parseImages(q.getImages()),
                    q.getDifficulty(),
                    optionsOf(q, ctx),
                    ctx.knowledgePoints.getOrDefault(q.getId(), List.of()),
                    ctx.favorited.contains(q.getId()),
                    q.getCorrectAnswer(),
                    q.getReferenceAnswer(),
                    parseStringArray(q.getScorePoints()),
                    q.getAnalysis(),
                    null,
                    null));
        }
        return result;
    }

    /**
     * 装配不含答案的题目（05 §5.2 {@link QuestionVO}），用于收藏列表与错题回顾。
     */
    public List<QuestionVO> toVOList(Long userId, List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Context ctx = loadContext(userId, questions);
        List<QuestionVO> result = new ArrayList<>(questions.size());
        for (Question q : questions) {
            result.add(new QuestionVO(
                    String.valueOf(q.getId()),
                    String.valueOf(q.getSubjectId()),
                    String.valueOf(q.getOutlineId()),
                    ctx.outlinePaths.getOrDefault(q.getOutlineId(), List.of()),
                    q.getQuestionType(),
                    q.getStem(),
                    parseImages(q.getImages()),
                    q.getDifficulty(),
                    optionsOf(q, ctx),
                    ctx.knowledgePoints.getOrDefault(q.getId(), List.of()),
                    ctx.favorited.contains(q.getId())));
        }
        return result;
    }

    /** 题目 ID → 章节路径名（如 {@code ["计算机系统基础知识","操作系统","进程管理"]}）。 */
    public Map<Long, List<String>> outlinePaths(Long subjectId) {
        Map<Long, List<String>> paths = new HashMap<>();
        for (OutlineNodeVO root : outlineService.getTree(subjectId, false)) {
            walkOutline(root, List.of(), paths);
        }
        return paths;
    }

    /**
     * 展开"某章节及其全部子孙"的 ID 集合，供 {@code CHAPTER} 练习组卷使用
     * （06 §6.6.3：{@code outline_id IN (该节点及其子孙)}）。
     *
     * <p>{@code rk_question.outline_id} 只挂叶子章节，所以用户在章节树上点一个非叶节点时，
     * 必须把子孙叶子一起带上，否则取到的是空池。
     *
     * <p><b>节点不存在时返回只含该 ID 的单元素列表</b>，不抛异常：后续 SQL 按这个 ID 过滤
     * 自然得到空池，前端落到"该范围暂无题目"的空态——这比抛一个 05 §5.5 未定义的错误码
     * 更贴近契约（该接口只列了 3001/1001/2005）。
     */
    public List<Long> outlineSubtreeIds(Long subjectId, Long rootId) {
        List<Long> ids = new ArrayList<>();
        for (OutlineNodeVO root : outlineService.getTree(subjectId, false)) {
            if (collectSubtree(root, rootId, ids)) {
                return ids;
            }
        }
        return List.of(rootId);
    }

    /**
     * 深度优先查找 {@code targetId}，命中后把**该节点及其全部子孙**收进 {@code out}。
     *
     * <p>注意不能写成"每个节点都判断自己是否等于目标"：那样只会收进目标节点本身，
     * 子孙一个都进不来——而"含子孙"正是这个方法的全部意义（非叶节点下挂的全是叶子题）。
     * 命中即整棵子树照单全收，并立刻返回，避免别的分支往 {@code out} 里混入无关 ID。
     *
     * @return 本次遍历是否命中目标
     */
    private static boolean collectSubtree(OutlineNodeVO node, Long targetId, List<Long> out) {
        if (targetId == null) {
            return false;
        }
        if (targetId.equals(parseId(node.id()))) {
            addSubtree(node, out);
            return true;
        }
        for (OutlineNodeVO child : node.children()) {
            if (collectSubtree(child, targetId, out)) {
                return true;
            }
        }
        return false;
    }

    /** 无条件收集该节点及其全部子孙的 ID。 */
    private static void addSubtree(OutlineNodeVO node, List<Long> out) {
        Long id = parseId(node.id());
        if (id != null) {
            out.add(id);
        }
        for (OutlineNodeVO child : node.children()) {
            addSubtree(child, out);
        }
    }

    private static Long parseId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------

    /** 一次批量查询装出来的四样关联数据。 */
    private record Context(Map<Long, List<QuestionOptionVO>> options,
                           Map<Long, List<QuestionKpVO>> knowledgePoints,
                           Set<Long> favorited,
                           Map<Long, List<String>> outlinePaths) {
    }

    private Context loadContext(Long userId, List<Question> questions) {
        List<Long> ids = questions.stream().map(Question::getId).toList();

        Map<Long, List<QuestionOptionVO>> options = new LinkedHashMap<>();
        for (QuestionOption option : questionMapper.selectOptionsByQuestionIds(ids)) {
            options.computeIfAbsent(option.getQuestionId(), key -> new ArrayList<>())
                    .add(new QuestionOptionVO(option.getOptionKey(), option.getContent()));
        }

        Map<Long, List<QuestionKpVO>> knowledgePoints = new LinkedHashMap<>();
        for (QuestionKpRowVO row : questionMapper.selectKnowledgePointsByQuestionIds(ids)) {
            knowledgePoints.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                    .add(new QuestionKpVO(String.valueOf(row.getKpId()), row.getKpName()));
        }

        // 游客没有收藏（05 §5.2：isFavorited 恒为 boolean，游客 false）
        Set<Long> favorited = userId == null
                ? Set.of()
                : Set.copyOf(userFavoriteMapper.selectFavoritedIds(userId, ids));

        // 一张卷子/一页收藏同属一个科目，因此只走一次大纲树（缓存 1 小时）
        Long subjectId = questions.get(0).getSubjectId();
        Map<Long, List<String>> outlinePaths = outlinePaths(subjectId);

        return new Context(options, knowledgePoints, favorited, outlinePaths);
    }

    private static void walkOutline(OutlineNodeVO node, List<String> prefix, Map<Long, List<String>> out) {
        List<String> path = new ArrayList<>(prefix);
        path.add(node.name());
        try {
            out.put(Long.valueOf(node.id()), List.copyOf(path));
        } catch (NumberFormatException ex) {
            // 章节 ID 非数字只可能来自人工改库；跳过这一枝，别让整页题目装配失败。
            log.warn("章节 id 不是数字，已跳过该节点的路径登记：id={}", node.id());
        }
        for (OutlineNodeVO child : node.children()) {
            walkOutline(child, path, out);
        }
    }

    /**
     * 解析 {@code rk_question.images}（JSON 数组文本）→ {@code [{url, alt}]}。
     *
     * <p>解析失败返回空列表并记 WARN，而不是抛异常：插图是题干 Markdown 的补充，
     * 一张图读不出来不该让整道题（以及整页卷子）打不开。
     */
    private static List<QuestionImageVO> parseImages(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JSONArray array = JSONUtil.parseArray(raw);
            List<QuestionImageVO> images = new ArrayList<>(array.size());
            for (Object item : array) {
                if (item instanceof JSONObject obj) {
                    images.add(new QuestionImageVO(obj.getStr("url"), obj.getStr("alt")));
                }
            }
            return images;
        } catch (Exception ex) {
            log.warn("题目 images 不是合法 JSON 数组，按无插图处理：raw={}", raw, ex);
            return List.of();
        }
    }

    /** 解析 {@code rk_question.score_points}（JSON 字符串数组文本）→ {@code List<String>}。 */
    private static List<String> parseStringArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JSONArray array = JSONUtil.parseArray(raw);
            List<String> values = new ArrayList<>(array.size());
            for (Object item : array) {
                values.add(item == null ? "" : String.valueOf(item));
            }
            return values;
        } catch (Exception ex) {
            log.warn("题目 score_points 不是合法 JSON 数组，按无评分要点处理：raw={}", raw, ex);
            return List.of();
        }
    }

    /**
     * 取选项。**主观题与论文恒为 {@code null}**（05 §5.2：{@code options} 仅客观题）。
     *
     * <p>不是"查不到就返回空数组"：空数组会让前端渲染出一个空的选择区，
     * 而 {@code null} 让它走进"不判分，以下为参考答案"那条分支——两者的界面完全不同，
     * 不能靠数据巧合来区分。
     */
    private static List<QuestionOptionVO> optionsOf(Question question, Context ctx) {
        if (!hasOptions(question.getQuestionType())) {
            return null;
        }
        return ctx.options.getOrDefault(question.getId(), List.of());
    }

    /** 供上层判断"这道题要不要给选项"：主观题与论文没有选项。 */
    public static boolean hasOptions(String questionType) {
        try {
            return QuestionType.valueOf(questionType).isObjective();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}