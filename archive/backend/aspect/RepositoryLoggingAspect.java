package org.example.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * AOP Aspect for logging all repository method calls.
 * Provides detailed debugging information including:
 * - Method name and parameters
 * - Execution time
 * - Return values
 * - Exceptions
 *
 * DEBUGGING: Set breakpoints in the following methods to intercept repository calls:
 * - beforeRepositoryMethod() - Breaks BEFORE any repository call
 * - afterRepositoryMethodReturning() - Breaks AFTER successful call
 * - afterRepositoryMethodThrowing() - Breaks AFTER exception
 * - aroundRepositoryMethod() - Breaks BEFORE and measures execution time
 */
@Aspect
// @Component  // DISABLED: Uncomment to enable repository logging for debugging
public class RepositoryLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryLoggingAspect.class);

    /**
     * BREAKPOINT HERE: Intercepts ALL repository method calls
     * Set a breakpoint on line 46 to debug ANY repository method execution
     */
    @Pointcut("execution(* org.example.repository..*(..))")
    public void repositoryMethods() {
    }

    /**
     * BREAKPOINT HERE: Logs before repository method execution
     * Set breakpoint on line 53 to inspect method parameters
     */
    @Before("repositoryMethods()")
    public void beforeRepositoryMethod(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        logger.debug("📥 [REPO-CALL] {}.{} called with {} parameters",
                className.substring(className.lastIndexOf('.') + 1),
                methodName,
                args.length);

        // Log each parameter with its type and value
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                String argInfo = formatArgument(arg, i);
                logger.debug("   └─ {}", argInfo);
            } else {
                logger.debug("   └─ Param[{}]: null", i);
            }
        }
    }

    /**
     * BREAKPOINT HERE: Logs successful repository method returns
     * Set breakpoint on line 80 to inspect return values
     */
    @AfterReturning(pointcut = "repositoryMethods()", returning = "result")
    public void afterRepositoryMethodReturning(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String resultInfo = formatResult(result);
        logger.debug("📤 [REPO-RETURN] {}.{} returned: {}",
                className.substring(className.lastIndexOf('.') + 1),
                methodName,
                resultInfo);
    }

    /**
     * BREAKPOINT HERE: Logs repository method exceptions
     * Set breakpoint on line 99 to debug query errors
     */
    @AfterThrowing(pointcut = "repositoryMethods()", throwing = "exception")
    public void afterRepositoryMethodThrowing(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        logger.error("❌ [REPO-ERROR] {}.{} threw exception: {}",
                className.substring(className.lastIndexOf('.') + 1),
                methodName,
                exception.getMessage(),
                exception);
    }

    /**
     * BREAKPOINT HERE: Measures execution time and logs performance
     * Set breakpoint on line 117 (before proceed) to pause before query execution
     * Set breakpoint on line 123 (after proceed) to pause after query execution
     */
    @Around("repositoryMethods()")
    public Object aroundRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        try {
            // BREAKPOINT: Set breakpoint here to pause BEFORE query execution
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // BREAKPOINT: Set breakpoint here to pause AFTER query execution
            logExecutionTime(className, methodName, executionTime, result);

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("⚠️ [REPO-PERF] {}.{} failed after {}ms",
                    className.substring(className.lastIndexOf('.') + 1),
                    methodName,
                    executionTime);
            throw throwable;
        }
    }

    /**
     * Logs execution time with performance warnings
     */
    private void logExecutionTime(String className, String methodName, long executionTime, Object result) {
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);
        String resultInfo = formatResult(result);

        if (executionTime > 1000) {
            // Slow query (> 1 second) - WARNING
            logger.warn("🐌 [REPO-SLOW] {}.{} took {}ms (SLOW!) → {}",
                    shortClassName, methodName, executionTime, resultInfo);
        } else if (executionTime > 500) {
            // Moderate query (> 500ms) - INFO
            logger.info("⏱️ [REPO-PERF] {}.{} took {}ms → {}",
                    shortClassName, methodName, executionTime, resultInfo);
        } else {
            // Fast query (< 500ms) - DEBUG
            logger.debug("⚡ [REPO-PERF] {}.{} took {}ms → {}",
                    shortClassName, methodName, executionTime, resultInfo);
        }
    }

    /**
     * Formats method arguments for logging
     */
    private String formatArgument(Object arg, int index) {
        if (arg == null) {
            return String.format("Param[%d]: null", index);
        }

        String typeName = arg.getClass().getSimpleName();

        // Handle different parameter types
        if (arg instanceof String) {
            return String.format("Param[%d] (String): \"%s\"", index, arg);
        } else if (arg instanceof Number) {
            return String.format("Param[%d] (%s): %s", index, typeName, arg);
        } else if (arg instanceof LocalDateTime) {
            return String.format("Param[%d] (LocalDateTime): %s", index, arg);
        } else if (arg instanceof Pageable) {
            Pageable pageable = (Pageable) arg;
            return String.format("Param[%d] (Pageable): page=%d, size=%d, sort=%s",
                    index, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        } else if (arg instanceof Enum) {
            return String.format("Param[%d] (%s): %s", index, typeName, arg);
        } else if (arg.getClass().getPackage() != null &&
                   arg.getClass().getPackage().getName().startsWith("org.example.entity")) {
            // Entity object - show ID if available
            try {
                Object id = arg.getClass().getMethod("getId").invoke(arg);
                return String.format("Param[%d] (%s): id=%s", index, typeName, id);
            } catch (Exception e) {
                return String.format("Param[%d] (%s): %s", index, typeName, arg);
            }
        } else {
            return String.format("Param[%d] (%s): %s", index, typeName, arg);
        }
    }

    /**
     * Formats method return values for logging
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        String typeName = result.getClass().getSimpleName();

        // Handle different return types
        if (result instanceof Optional) {
            Optional<?> optional = (Optional<?>) result;
            if (optional.isPresent()) {
                Object value = optional.get();
                String valueType = value.getClass().getSimpleName();
                try {
                    Object id = value.getClass().getMethod("getId").invoke(value);
                    return String.format("Optional[%s(id=%s)]", valueType, id);
                } catch (Exception e) {
                    return String.format("Optional[%s]", valueType);
                }
            } else {
                return "Optional.empty";
            }
        } else if (result instanceof Page) {
            Page<?> page = (Page<?>) result;
            return String.format("Page[%d elements, page %d/%d, total %d]",
                    page.getNumberOfElements(),
                    page.getNumber() + 1,
                    page.getTotalPages(),
                    page.getTotalElements());
        } else if (result instanceof Collection) {
            Collection<?> collection = (Collection<?>) result;
            return String.format("%s[%d elements]", typeName, collection.size());
        } else if (result instanceof Boolean) {
            return result.toString();
        } else if (result instanceof Number) {
            return result.toString();
        } else if (result.getClass().isArray()) {
            return String.format("Array[%d elements]", Arrays.asList((Object[]) result).size());
        } else if (result.getClass().getPackage() != null &&
                   result.getClass().getPackage().getName().startsWith("org.example.entity")) {
            // Entity object - show ID if available
            try {
                Object id = result.getClass().getMethod("getId").invoke(result);
                return String.format("%s(id=%s)", typeName, id);
            } catch (Exception e) {
                return typeName;
            }
        } else {
            return typeName;
        }
    }
}
