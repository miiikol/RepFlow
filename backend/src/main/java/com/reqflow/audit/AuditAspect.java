package com.reqflow.audit;

import com.reqflow.common.Result;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.AuditLog;
import com.reqflow.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogWriter auditLogWriter;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        String objectId = resolveObjectId(joinPoint, audit);
        boolean success = true;
        String error = null;
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Result<?> r) {
                if (objectId == null && r.getData() instanceof Number n) {
                    objectId = String.valueOf(n.longValue());
                }
                if (r.getCode() != 0) {
                    success = false;
                    error = r.getMessage();
                }
            }
            return result;
        } catch (Throwable t) {
            success = false;
            error = t.getMessage();
            throw t;
        } finally {
            save(audit.module(), audit.action(), audit.objectType(), objectId, success, error,
                    System.currentTimeMillis() - start);
        }
    }

    private String resolveObjectId(ProceedingJoinPoint joinPoint, Audit audit) {
        if (audit.objectIdParam().isBlank()) {
            return null;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(audit.objectIdParam()) && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    /** 供业务服务在状态流转、验收等关键动作中手工记录审计 */
    public void record(String module, String action, String objectType, String objectId) {
        save(module, action, objectType, objectId, true, null, 0);
    }

    private void save(String module, String action, String objectType, String objectId,
                      boolean success, String error, long duration) {
        try {
            AuditLog log = new AuditLog();
            try {
                LoginUser user = SecurityUtils.currentUser();
                log.setUserId(user.getId());
                log.setUsername(user.getUsername());
                log.setRoleCodes(String.join(",", user.getRoles()));
            } catch (Exception ignored) {
                log.setUsername("anonymous");
            }
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.setIp(request.getRemoteAddr());
                log.setUri(request.getRequestURI());
                log.setMethod(request.getMethod());
            }
            log.setModule(module);
            log.setAction(action);
            log.setObjectType(objectType);
            log.setObjectId(objectId);
            log.setResult(success ? "SUCCESS" : "FAIL");
            log.setErrorMsg(error == null ? null : error.substring(0, Math.min(error.length(), 480)));
            log.setDurationMs(duration);
            log.setTraceId(UUID.randomUUID().toString().substring(0, 8));
            log.setCreatedAt(LocalDateTime.now());
            auditLogWriter.write(log);
        } catch (Exception ignored) {
            // 审计失败不影响主流程
        }
    }
}
