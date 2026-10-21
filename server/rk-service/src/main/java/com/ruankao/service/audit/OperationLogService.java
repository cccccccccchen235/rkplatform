package com.ruankao.service.audit;

/**
 * 高危操作留痕（03 §3.8 <b>AC-2</b>）。
 *
 * <p>放在 {@code audit} 包而不是某个业务包内：留痕是被<b>所有</b>业务域调用的一件事
 * （P2 的科目停用、将来的题目下架 / 用户禁用 / 配置修改）。放进 {@code question} 包会让
 * {@code user} 包为了记一条日志而依赖 {@code question}，那是条假依赖——
 * 两者之间没有业务关系，只有"都要写审计"这一个共同需求。
 * 03 §3.3.2 的六个包是业务域清单，交叉的横切能力不在这张表上。
 *
 * <p>本接口只有写入，没有读取：P2 没有查询审计日志的接口（05 §5.10 未定义），
 * 不预留查询方法以免"看起来已经能看审计了"。
 */
public interface OperationLogService {

    /**
     * 记一条操作日志。
     *
     * <p><b>绝不写入密码、token、答案正文</b>（AC-3）：{@code detail} 只放业务键与结果摘要，
     * 例如 {@code "科目 2 (SAD) 状态 ENABLED → DISABLED"}。写入失败会抛出——
     * AC-2 要求的是"高危操作<b>必须</b>留痕"，静默吞掉写日志的异常等于把这条约束关掉，
     * 而且看不出来（连同事务一起回滚，正是想要的效果）。
     *
     * @param operatorId 操作者用户 ID（AC-3：用户数据访问必须携带 user_id）
     * @param module     QUESTION / REVIEW / SUBJECT / USER / CONFIG（04 §4.9）
     * @param action     CREATE / UPDATE / DELETE / PUBLISH / OFFLINE / RESET_PASSWORD / DISABLE
     * @param detail     结果摘要；调用方负责不超过 1000 字符（DDL 列宽）
     * @param ip         来源 IP，可为 null
     */
    void record(Long operatorId,
                String module,
                String action,
                String targetType,
                Long targetId,
                String detail,
                String ip);
}
