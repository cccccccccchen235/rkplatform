package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志写入（03 §3.8 <b>AC-2</b>）。
 *
 * <p>只有 {@code insert}：P2 只要求"高危操作留痕"，05 §5.10 里没有查询审计日志的接口。
 * 不预先提供查询方法——一个没有调用方的查询接口只会让人以为"审计已经能看了"。
 *
 * <p>SQL 在 {@code resources/mapper/SysOperationLogMapper.xml}。
 */
@Mapper
public interface SysOperationLogMapper {

    /** 写入一条审计记录；自增主键回填。 */
    int insert(SysOperationLog operationLog);
}
