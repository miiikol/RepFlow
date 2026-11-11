package com.reqflow.audit;

import com.reqflow.domain.entity.AuditLog;
import com.reqflow.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志写入器：使用独立事务提交。
 * <p>
 * 业务事务回滚时，操作留痕仍然保留；业务事务持有行锁也不会影响日志表写入。
 * 写入失败由调用方捕获，不影响主流程。
 */
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogMapper auditLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditLog log) {
        auditLogMapper.insert(log);
    }
}
