package com.moviebooking.modules.audit.aspect;

import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.audit.dto.request.AuditEntry;
import com.moviebooking.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/** Xử lý {@link Audited}: ghi nhật ký sau khi method trả về thành công. */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    private final AuditService auditService;

    @AfterReturning("@annotation(audited)")
    public void afterSuccess(JoinPoint joinPoint, Audited audited) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        var context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), signature.getMethod(), joinPoint.getArgs(), PARAMETER_NAMES);
        Object resourceId = evaluate(audited.resourceId(), context);
        Object data = evaluate(audited.data(), context);
        auditService.record(AuditEntry.of(
                audited.action(), audited.resource(), resourceId == null ? null : String.valueOf(resourceId), data));
    }

    private static Object evaluate(String expression, MethodBasedEvaluationContext context) {
        return expression.isBlank() ? null : PARSER.parseExpression(expression).getValue(context);
    }
}
