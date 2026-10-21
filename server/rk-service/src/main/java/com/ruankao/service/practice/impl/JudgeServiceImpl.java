package com.ruankao.service.practice.impl;

import com.ruankao.domain.entity.Question;
import com.ruankao.domain.enums.QuestionType;
import com.ruankao.service.practice.JudgeResult;
import com.ruankao.service.practice.JudgeService;
import java.util.Arrays;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 判分实现（06 §6.7）。
 *
 * <p>三条口径，都直接对应文档：
 *
 * <ul>
 *   <li><b>单选 / 判断</b>：归一化后严格相等。</li>
 *   <li><b>多选</b>：拆成字符集合比较，顺序无关，<b>完全相等才得分</b>——漏选、多选都不给部分分
 *       （与官方一致，06 §6.7）。</li>
 *   <li><b>主观题 / 论文</b>：不判分，返回 {@link JudgeResult#unscored()}。</li>
 * </ul>
 *
 * <p>归一化 = 去首尾空白 + 转大写。空串视为未作答（{@link JudgeResult#blankAnswer()}）——
 * 它同样得 0 分，但调用方据此跳过错题本，这正是"没做完"与"不会做"的区别（06 §6.8）。
 */
@Service
public class JudgeServiceImpl implements JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeServiceImpl.class);

    @Override
    public JudgeResult judge(Question question, String userAnswer) {
        QuestionType type = parseType(question.getQuestionType());
        if (type == null || !type.isObjective()) {
            // 主观题、论文，或库里的 question_type 不在枚举内。两者都返回"不判分"：
            // 拿不准是不是客观题时，宁可让这道题不进统计，也不能凭一个读不懂的类型
            // 造出一个"答错了"的结论——那会污染错题本与正确率。
            return JudgeResult.unscored();
        }

        String normalizedAnswer = normalize(userAnswer);
        if (normalizedAnswer.isEmpty()) {
            return JudgeResult.blankAnswer();
        }

        String expected = normalize(question.getCorrectAnswer());
        boolean correct = switch (type) {
            case SINGLE_CHOICE, TRUE_FALSE -> normalizedAnswer.equals(expected);
            case MULTI_CHOICE -> sortedChars(normalizedAnswer).equals(sortedChars(expected));
            // 不可达：上面已用 isObjective() 挡掉其余两种；写出来是为了让 switch 穷尽，
            // 将来新增题型时编译器会在这里提醒。
            case SUBJECTIVE, ESSAY -> false;
        };
        return correct ? JudgeResult.correct() : JudgeResult.wrong();
    }

    /**
     * 解析题型。解析失败返回 {@code null}（由调用方判成"不判分"）并记 WARN。
     *
     * <p>不直接 {@code QuestionType.valueOf}：它对未知值抛 {@code IllegalArgumentException}，
     * 会把一道脏数据的题目变成整批上报 500，而上传接口是批量事务——
     * 一颗坏行不该让用户这一轮作答全部丢失。同样的处置见
     * {@code SubjectServiceImpl#loadAllGrouped}（level 落在枚举外时跳过并留痕）。
     */
    private static QuestionType parseType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return QuestionType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("题目 question_type 不在 QuestionType 枚举内，按不判分处理：type={}", raw);
            return null;
        }
    }

    /** 去首尾空白 + 转大写；{@code null} 归成空串，让"没传"与"传了空"走同一条分支。 */
    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 多选答案 → 排序后的字符序列，用于集合比较（顺序无关）。
     *
     * <p>顺带去掉内部空白，使 {@code "A B D"} 与 {@code "ABD"} 等价——前端多个控件拼出来的
     * 答案不必与库里的紧凑格式逐字一致。
     */
    private static String sortedChars(String normalized) {
        char[] chars = normalized.replaceAll("\\s+", "").toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}