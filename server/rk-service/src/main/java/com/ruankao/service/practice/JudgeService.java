package com.ruankao.service.practice;

import com.ruankao.domain.entity.Question;

/**
 * 判分（06 §6.7）。
 *
 * <p><b>服务端权威的落点。</b>练习模式为了省掉每题一次往返，把答案随题目一起下发给前端，
 * 由前端即时判分给用户看（05 §5.5）。那条路径上的 {@code isCorrect} **不可信**——
 * 它是浏览器算出来的，用户可以随便改。所以答题结果上报时（{@code POST /practice/records}）
 * 服务端必须用本接口**重新判一遍**，并以这里的结果落库、更新错题本与统计
 * （验收项 AC-3，见 10 §10.6）。
 *
 * <p>本接口刻意做成无状态纯函数：判分只依赖题目与用户作答两个入参，
 * 不碰数据库、不碰 Redis。这样它可以用纯单测穷举边界（06 §6.14 的测试要求），
 * 也不会因为一次"顺手查一下"而在模拟考的交卷事务里引入额外往返。
 */
public interface JudgeService {

    /**
     * 判一道题。
     *
     * @param question   题目实体；只用它的 {@code questionType} 与 {@code correctAnswer}
     * @param userAnswer 用户作答原文；{@code null} 与空白串都表示未作答
     * @return 判分结果，恒非 {@code null}
     */
    JudgeResult judge(Question question, String userAnswer);
}