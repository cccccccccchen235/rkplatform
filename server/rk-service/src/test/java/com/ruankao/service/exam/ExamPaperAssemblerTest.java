package com.ruankao.service.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruankao.domain.vo.ExamQuestionCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 组卷算法单测（06 §6.6.2）：配额分配、难度分档、L2 复用、L4 缩减。 */
class ExamPaperAssemblerTest {

    @Test
    @DisplayName("Step A：每章保底 1 题，其余按题量用最大余数法分配")
    void quotasUseLargestRemainder() {
        Map<Long, Integer> sizes = new LinkedHashMap<>();
        sizes.put(1L, 10);
        sizes.put(2L, 30);

        Map<Long, Integer> quotas = ExamPaperAssembler.allocateQuotas(10, sizes, new Random(1));

        assertEquals(3, quotas.get(1L));
        assertEquals(7, quotas.get(2L));
        assertEquals(10, quotas.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    @DisplayName("题量不足以覆盖所有章节时，按题量取前 N 个章节各 1 题")
    void smallPaperCoversBiggestOutlines() {
        Map<Long, Integer> sizes = new LinkedHashMap<>();
        sizes.put(1L, 5);
        sizes.put(2L, 20);
        sizes.put(3L, 40);

        Map<Long, Integer> quotas = ExamPaperAssembler.allocateQuotas(2, sizes, new Random(1));

        assertEquals(Set.of(3L, 2L), quotas.keySet());
        assertTrue(quotas.values().stream().allMatch(v -> v == 1));
    }

    @Test
    @DisplayName("优先抽未做过的题；不足时才复用窗口内旧题并计入 repeatedCount")
    void reuseIsCountedButNeverPreferred() {
        // 章节 1 有 3 题，其中 2 题在排除集里
        List<ExamQuestionCandidate> candidates = new ArrayList<>(List.of(
                candidate(101L, 1L, 3), candidate(102L, 1L, 3), candidate(103L, 1L, 3)));

        ExamPaperAssembler.Selection fresh = ExamPaperAssembler.select(
                candidates, 1, Set.of(101L, 102L), new Random(1));
        assertEquals(List.of(103L), fresh.questionIds());
        assertEquals(0, fresh.repeatedCount());

        ExamPaperAssembler.Selection reused = ExamPaperAssembler.select(
                candidates, 3, Set.of(101L, 102L), new Random(1));
        assertEquals(3, reused.questionIds().size());
        assertEquals(2, reused.repeatedCount());
        assertTrue(reused.questionIds().containsAll(List.of(101L, 102L, 103L)));
    }

    @Test
    @DisplayName("L4：候选不足时缩减题量并报告未覆盖章节")
    void shortageShrinksAndReportsUncovered() {
        // 章节 1 只有 1 题，章节 2 只有 1 题，要求 5 题
        List<ExamQuestionCandidate> candidates = List.of(
                candidate(201L, 1L, 2), candidate(202L, 2L, 4));

        ExamPaperAssembler.Selection selection = ExamPaperAssembler.select(
                candidates, 5, Set.of(), new Random(1));

        assertEquals(2, selection.questionIds().size());
        assertTrue(selection.uncoveredOutlineIds().isEmpty());
    }

    @Test
    @DisplayName("本章节新题全部在窗口内时，L2 仍能复用旧题（回归：曾在此抛 UnsupportedOperationException）")
    void allExcludedStillFallsBackToReuse() {
        List<ExamQuestionCandidate> candidates = List.of(
                candidate(301L, 1L, 3), candidate(302L, 1L, 3));

        ExamPaperAssembler.Selection selection = ExamPaperAssembler.select(
                candidates, 2, Set.of(301L, 302L), new Random(1));

        assertEquals(2, selection.questionIds().size());
        assertEquals(2, selection.repeatedCount());
    }

    @Test
    @DisplayName("空候选集返回空结果，不抛异常")
    void emptyCandidatesGiveEmptySelection() {
        ExamPaperAssembler.Selection selection = ExamPaperAssembler.select(List.of(), 10, Set.of(), new Random(1));
        assertTrue(selection.questionIds().isEmpty());
        assertEquals(0, selection.repeatedCount());
        assertTrue(selection.uncoveredOutlineIds().isEmpty());
    }

    private static ExamQuestionCandidate candidate(Long questionId, Long outlineId, int difficulty) {
        ExamQuestionCandidate candidate = new ExamQuestionCandidate();
        candidate.setQuestionId(questionId);
        candidate.setOutlineId(outlineId);
        candidate.setDifficulty(difficulty);
        return candidate;
    }
}
