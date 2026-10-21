package com.ruankao.service.exam;

import com.ruankao.domain.vo.ExamQuestionCandidate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 模拟考组卷（06 §6.6.2）。纯函数：不碰数据库、不碰 Redis，随机源由调用方注入，因此可单测。
 *
 * <p>算法分四步，降级优先级是 <b>不重复作答 &gt; 章节覆盖 &gt; 难度配比 &gt; 题量达标</b>：
 * <ol>
 *   <li><b>Step A 配额</b>：有题的章节先各保底 1 题，剩余按题量占比用最大余数法分配。</li>
 *   <li><b>Step B 抽题</b>：章节内按难度三档（基础 30% / 进阶 50% / 挑战 20%）抽取，
 *       某档不足由相邻档补足。</li>
 *   <li><b>L2 放宽防重复</b>：本章节新题用尽时，按"最早做过的先复用"补足，计入 repeatedCount。</li>
 *   <li><b>L3 跨章节</b>：仍不足时从其他章节补；<b>L4 缩减</b>：再不足就少出题，并报告未覆盖章节。</li>
 * </ol>
 *
 * <p>注意 06 §6.6.2 的决策段写"客观题总量不足时直接 4002、不生成残缺试卷"，而同节 6.9.3 的
 * 校验表与 10 §10.7 的验收项都要求"按 L4 缩减并提示"。以后两者为准——验收项是权威
 * （本处不一致已登记，见 docs/30）。
 */
public final class ExamPaperAssembler {

    /** 返回：抽中的题 ID（按章节配额顺序）、复用了窗口内旧题的条数、未被覆盖的章节。 */
    public record Selection(List<Long> questionIds, int repeatedCount, List<Long> uncoveredOutlineIds) {
    }

    private static final int[][] BANDS = {{1, 2}, {3, 3}, {4, 5}};
    private static final double[] BAND_RATIOS = {0.30, 0.50, 0.20};
    /** 某档不足时的补足顺序：先进阶，再基础，最后挑战（06 §6.6.2 的难度分档说明）。 */
    private static final int[] FILL_ORDER = {1, 0, 2};

    private ExamPaperAssembler() {
    }

    public static Selection select(List<ExamQuestionCandidate> candidates,
                                   int required,
                                   Set<Long> excluded,
                                   Random random) {
        if (required <= 0 || candidates == null || candidates.isEmpty()) {
            return new Selection(List.of(), 0, List.of());
        }

        Map<Long, List<ExamQuestionCandidate>> byOutline = new LinkedHashMap<>();
        for (ExamQuestionCandidate candidate : candidates) {
            byOutline.computeIfAbsent(candidate.getOutlineId(), k -> new ArrayList<>()).add(candidate);
        }

        Set<Long> picked = new LinkedHashSet<>();
        Map<Long, Integer> quotas = allocateQuotas(required, sizes(byOutline), random);

        // Step B + L2：逐章节抽题
        for (Map.Entry<Long, Integer> entry : quotas.entrySet()) {
            List<ExamQuestionCandidate> pool = byOutline.get(entry.getKey());
            int quota = entry.getValue();

            List<ExamQuestionCandidate> fresh = new ArrayList<>();
            for (ExamQuestionCandidate candidate : pool) {
                if (!excluded.contains(candidate.getQuestionId())) {
                    fresh.add(candidate);
                }
            }
            // 必须包一层可变列表：pickByBand 在"没有新题可用"时返回 List.of()（不可变），
            // 下面的 L2 分支要往里 add，直接用它会在首次触发降级时抛 UnsupportedOperationException。
            List<Long> chosen = new ArrayList<>(pickByBand(fresh, quota, random));

            if (chosen.size() < quota) {
                // L2：放宽防重复，按"最早做过的先复用"；从未做过的排在最后（nulls last）
                List<ExamQuestionCandidate> leftovers = new ArrayList<>();
                for (ExamQuestionCandidate candidate : pool) {
                    if (!chosen.contains(candidate.getQuestionId())) {
                        leftovers.add(candidate);
                    }
                }
                leftovers.sort(Comparator
                        .comparing(ExamQuestionCandidate::getLastAnsweredAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ExamQuestionCandidate::getQuestionId));
                for (ExamQuestionCandidate candidate : leftovers) {
                    if (chosen.size() >= quota) {
                        break;
                    }
                    chosen.add(candidate.getQuestionId());
                }
            }
            picked.addAll(chosen);
        }

        // L3：跨章节补足（优先未在排除集里的题，其次按最早做过）
        if (picked.size() < required) {
            List<ExamQuestionCandidate> rest = new ArrayList<>();
            for (ExamQuestionCandidate candidate : candidates) {
                if (!picked.contains(candidate.getQuestionId())) {
                    rest.add(candidate);
                }
            }
            rest.sort(Comparator
                    .comparing((ExamQuestionCandidate c) -> excluded.contains(c.getQuestionId()))
                    .thenComparing(ExamQuestionCandidate::getLastAnsweredAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ExamQuestionCandidate::getQuestionId));
            for (ExamQuestionCandidate candidate : rest) {
                if (picked.size() >= required) {
                    break;
                }
                picked.add(candidate.getQuestionId());
            }
        }

        // L4：题量不足就缩减，并报告一个题都没出的章节
        int repeated = 0;
        for (Long id : picked) {
            if (excluded.contains(id)) {
                repeated++;
            }
        }
        List<Long> uncovered = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quotas.entrySet()) {
            boolean any = byOutline.get(entry.getKey()).stream()
                    .anyMatch(c -> picked.contains(c.getQuestionId()));
            if (!any) {
                uncovered.add(entry.getKey());
            }
        }
        return new Selection(new ArrayList<>(picked), repeated, uncovered);
    }

    /** 章节配额：保底 + 最大余数（06 §6.6.2 Step A）。 */
    static Map<Long, Integer> allocateQuotas(int required, Map<Long, Integer> sizes, Random random) {
        Map<Long, Integer> quotas = new LinkedHashMap<>();
        List<Long> outlines = new ArrayList<>(sizes.keySet());
        if (outlines.isEmpty()) {
            return quotas;
        }

        if (required < outlines.size()) {
            // 题量不够覆盖所有章节：按题量占比（最大余数）取前 required 个章节，各 1 题
            long total = 0;
            for (int size : sizes.values()) {
                total += size;
            }
            final long weightTotal = Math.max(1, total);
            // 先洗牌、再按题量稳定排序：并列时保留洗牌顺序。
            // 不能在 Comparator 里调 random——比较函数不确定会让 TimSort 抛
            // "Comparison method violates its general contract"。
            Collections.shuffle(outlines, random);
            outlines.sort(Comparator
                    .comparingDouble((Long id) -> -(required * (double) sizes.get(id) / weightTotal)));
            for (int i = 0; i < required; i++) {
                quotas.put(outlines.get(i), 1);
            }
            return quotas;
        }

        for (Long outline : outlines) {
            quotas.put(outline, 1);
        }
        distribute(quotas, sizes, required - outlines.size(), random);
        return quotas;
    }

    /** 最大余数法把 extra 个名额按题量占比分配下去。 */
    private static void distribute(Map<Long, Integer> quotas,
                                   Map<Long, Integer> sizes,
                                   int extra,
                                   Random random) {
        if (extra <= 0) {
            return;
        }
        long total = 0;
        for (int size : sizes.values()) {
            total += size;
        }
        if (total <= 0) {
            return;
        }

        Map<Long, Double> fractions = new HashMap<>();
        int assigned = 0;
        for (Map.Entry<Long, Integer> entry : sizes.entrySet()) {
            double exact = extra * (double) entry.getValue() / total;
            int whole = (int) Math.floor(exact);
            quotas.merge(entry.getKey(), whole, Integer::sum);
            assigned += whole;
            fractions.put(entry.getKey(), exact - whole);
        }

        int leftover = extra - assigned;
        if (leftover <= 0) {
            return;
        }
        List<Long> order = new ArrayList<>(sizes.keySet());
        Collections.shuffle(order, random);
        order.sort(Comparator.comparingDouble((Long id) -> -fractions.getOrDefault(id, 0.0)));
        for (int i = 0; i < leftover && i < order.size(); i++) {
            quotas.merge(order.get(i), 1, Integer::sum);
        }
    }

    /** 章节内按难度三档抽取（06 §6.6.2 Step B）。 */
    private static List<Long> pickByBand(List<ExamQuestionCandidate> pool, int quota, Random random) {
        if (quota <= 0 || pool.isEmpty()) {
            return List.of();
        }
        List<List<ExamQuestionCandidate>> bands = new ArrayList<>(BANDS.length);
        for (int[] band : BANDS) {
            List<ExamQuestionCandidate> list = new ArrayList<>();
            for (ExamQuestionCandidate candidate : pool) {
                Integer difficulty = candidate.getDifficulty();
                if (difficulty != null && difficulty >= band[0] && difficulty <= band[1]) {
                    list.add(candidate);
                }
            }
            Collections.shuffle(list, random);
            bands.add(list);
        }

        List<Long> chosen = new ArrayList<>(quota);
        int[] cursor = new int[BANDS.length];
        int[] want = new int[BANDS.length];
        for (int i = 0; i < BANDS.length; i++) {
            want[i] = (int) Math.floor(quota * BAND_RATIOS[i]);
        }
        want[BANDS.length - 1] = quota - want[0] - want[1];

        for (int i = 0; i < BANDS.length; i++) {
            int take = Math.min(want[i], bands.get(i).size());
            for (int k = 0; k < take; k++) {
                chosen.add(bands.get(i).get(k).getQuestionId());
            }
            cursor[i] = take;
        }

        int remaining = quota - chosen.size();
        for (int bandIndex : FILL_ORDER) {
            if (remaining <= 0) {
                break;
            }
            List<ExamQuestionCandidate> band = bands.get(bandIndex);
            while (cursor[bandIndex] < band.size() && remaining > 0) {
                chosen.add(band.get(cursor[bandIndex]).getQuestionId());
                cursor[bandIndex]++;
                remaining--;
            }
        }
        return chosen;
    }

    private static Map<Long, Integer> sizes(Map<Long, List<ExamQuestionCandidate>> byOutline) {
        Map<Long, Integer> sizes = new LinkedHashMap<>();
        byOutline.forEach((outlineId, list) -> sizes.put(outlineId, list.size()));
        return sizes;
    }
}
