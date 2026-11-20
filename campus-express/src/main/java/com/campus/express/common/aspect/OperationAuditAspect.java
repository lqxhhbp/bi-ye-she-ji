package com.campus.express.common.aspect;

import com.campus.express.common.annotation.OperationAudit;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import com.campus.express.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 关键写操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationAuditAspect {

    private final OperationLogService operationLogService;

    @Around("@annotation(operationAudit)")
    public Object record(ProceedingJoinPoint joinPoint, OperationAudit operationAudit) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            AuthUser user = UserContext.get();
            HttpServletRequest request = currentRequest();
            String requestDetail = request == null
                    ? joinPoint.getSignature().toShortString()
                    : request.getMethod() + " " + request.getRequestURI();
            operationLogService.record(
                    user == null ? null : user.userId(),
                    operationAudit.module(),
                    operationAudit.action(),
                    requestDetail,
                    request == null ? null : request.getRemoteAddr()
            );
        } catch (Exception exception) {
            log.warn("操作日志写入失败，不影响业务结果：{}", exception.getMessage());
        }
        return result;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
