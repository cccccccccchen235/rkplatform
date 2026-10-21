package com.ruankao.service.audit.impl;

import com.ruankao.domain.entity.SysOperationLog;
import com.ruankao.repository.mapper.SysOperationLogMapper;
import com.ruankao.service.audit.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    /** DDL 列为 VARCHAR(1000)；超出会让整条 INSERT 失败（严格模式下报错而非截断）。 */
    private static final int DETAIL_MAX_LENGTH = 1000;

    private final SysOperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * <b>不加 {@code @Transactional}</b>：本方法由调用方的业务事务包裹，
     * 自己也开一个事务只会在"业务回滚"时多一层无意义的参与。
     * 反过来，若调用方没有事务，这里也不需要——一条 INSERT 自带原子性。
     */
    @Override
    public void record(Long operatorId,
                       String module,
                       String action,
                       String targetType,
                       Long targetId,
                       String detail,
                       String ip) {
        SysOperationLog entry = new SysOperationLog();
        entry.setUserId(operatorId);
        entry.setModule(module);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetail(truncate(detail));
        entry.setIp(ip);

        operationLogMapper.insert(entry);

        // 审计写入的 INFO 日志：它<b>不是</b>审计本身（真值在库里），
        // 但让"停用科目"这件事在应用日志里也留一道痕迹，排查时不必先去连库。
        log.info("操作留痕：operator={}, module={}, action={}, target={}#{}, detail={}",
                operatorId, module, action, targetType, targetId, entry.getDetail());
    }

    /**
     * 截断到列宽。
     *
     * <p>宁可截断也不抛异常：摘要超长是调用方拼串时的疏忽，而为了一个"日志太长"
     * 让整个业务操作失败（比如科目没停用成功）是本末倒置。截断后的尾巴加省略号，
     * 让读到它的人知道后面还有内容。
     */
    private static String truncate(String detail) {
        if (detail == null || detail.length() <= DETAIL_MAX_LENGTH) {
            return detail;
        }
        return detail.substring(0, DETAIL_MAX_LENGTH - 3) + "...";
    }
}
