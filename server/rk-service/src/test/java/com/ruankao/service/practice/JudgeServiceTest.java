package com.ruankao.service.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruankao.domain.entity.Question;
import com.ruankao.service.practice.impl.JudgeServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 判分（{@link JudgeServiceImpl}）。
 *
 * <p>这批断言覆盖 06 §6.14 点名要求的四种题型与多选的三种错法（顺序不同 / 漏选 / 多选）。
 *
 * <p>为什么值得单独写一批纯单测：判分是**服务端权威**的最后一道闸（AC-3）。练习模式下
 * 答案随题目一起发给浏览器，前端算出来的对错可以被随意篡改；这条防线一旦口径错了，
 * 错的是错题本、正确率、薄弱知识点——一整条统计链，而界面上看不出任何异常。
 * 所以每一条分支都在这里钉死，不靠"跑一遍接口看着像对的"。
 */
class JudgeServiceTest {

    private final JudgeService judgeService = new JudgeServiceImpl();

    private static Question question(String type, String correctAnswer) {
        Question question = new Question();
        question.setId(1L);
        question.setSubjectId(6L);
        question.setQuestionType(type);
        question.setCorrectAnswer(correctAnswer);
        return question;
    }

    // ---------------- 单选 ----------------

    @Test
    @DisplayName("单选：选项相同判对，得 1.0 分")
    void singleChoiceCorrect() {
        JudgeResult result = judgeService.judge(question("SINGLE_CHOICE", "B"), "B");

        assertEquals(Boolean.TRUE, result.isCorrect());
        assertEquals(0, new BigDecimal("1.0").compareTo(result.score()));
        assertFalse(result.blank());
    }

    @Test
    @DisplayName("单选：选项不同判错，得 0 分且不是空作答")
    void singleChoiceWrong() {
        JudgeResult result = judgeService.judge(question("SINGLE_CHOICE", "B"), "C");

        assertEquals(Boolean.FALSE, result.isCorrect());
        assertEquals(0, new BigDecimal("0.0").compareTo(result.score()));
        assertFalse(result.blank(), "答错要进错题本，不能和『没作答』混为一谈");
    }

    @Test
    @DisplayName("单选：大小写与首尾空白归一化后仍判对")
    void singleChoiceNormalized() {
        JudgeResult result = judgeService.judge(question("SINGLE_CHOICE", "B"), "  b  ");

        assertEquals(Boolean.TRUE, result.isCorrect());
    }

    // ---------------- 判断 ----------------

    @Test
    @DisplayName("判断：与单选同一条比较路径，对错各一")
    void trueFalse() {
        assertEquals(Boolean.TRUE, judgeService.judge(question("TRUE_FALSE", "T"), "t").isCorrect());
        assertEquals(Boolean.FALSE, judgeService.judge(question("TRUE_FALSE", "T"), "F").isCorrect());
    }

    // ---------------- 多选 ----------------

    @Test
    @DisplayName("多选：顺序不同仍判对（集合比较，顺序无关）")
    void multiChoiceOrderInsensitive() {
        JudgeResult result = judgeService.judge(question("MULTI_CHOICE", "ABD"), "DBA");

        assertEquals(Boolean.TRUE, result.isCorrect());
    }

    @Test
    @DisplayName("多选：含空格的写法与紧凑写法等价")
    void multiChoiceIgnoresInnerSpaces() {
        JudgeResult result = judgeService.judge(question("MULTI_CHOICE", "ABD"), "A B D");

        assertEquals(Boolean.TRUE, result.isCorrect());
    }

    @Test
    @DisplayName("多选：漏选不给部分分，判错")
    void multiChoiceMissingOptionIsWrong() {
        JudgeResult result = judgeService.judge(question("MULTI_CHOICE", "ABD"), "AB");

        assertEquals(Boolean.FALSE, result.isCorrect());
    }

    @Test
    @DisplayName("多选：多选不给部分分，判错")
    void multiChoiceExtraOptionIsWrong() {
        JudgeResult result = judgeService.judge(question("MULTI_CHOICE", "ABD"), "ABCD");

        assertEquals(Boolean.FALSE, result.isCorrect());
    }

    // ---------------- 未作答 ----------------

    @Test
    @DisplayName("未作答：null 与空白串都判 false、得 0 分，且 blank=true（据此不入错题本）")
    void blankAnswers() {
        for (String raw : new String[] {null, "", "   "}) {
            JudgeResult result = judgeService.judge(question("SINGLE_CHOICE", "B"), raw);

            assertEquals(Boolean.FALSE, result.isCorrect());
            assertEquals(0, new BigDecimal("0.0").compareTo(result.score()));
            assertTrue(result.blank(), "未作答必须能被调用方识别出来：入参=" + raw);
        }
    }

    // ---------------- 主观题 / 论文 ----------------

    @Test
    @DisplayName("主观题与论文：isCorrect 为 null（不判分），得 0 分")
    void subjectiveIsUnscored() {
        for (String type : new String[] {"SUBJECTIVE", "ESSAY"}) {
            JudgeResult result = judgeService.judge(question(type, null), "随便写点什么");

            assertNull(result.isCorrect(), "不判分不是判错：type=" + type);
            assertEquals(0, new BigDecimal("0.0").compareTo(result.score()));
            assertFalse(result.scored(), "scored() 必须与 isCorrect 的口径一致：type=" + type);
        }
    }

    // ---------------- 脏数据 ----------------

    @Test
    @DisplayName("题型不在枚举内：按不判分兜底，而不是抛异常或判错")
    void unknownTypeIsUnscored() {
        JudgeResult result = judgeService.judge(question("MULTIPLE_CHOICE_SINGLE", "B"), "B");

        assertNull(result.isCorrect(), "读不懂的题型不能让一颗坏行毁掉整批上报，也不能凭空判错");
    }
}