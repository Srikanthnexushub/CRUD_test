package org.example.versioning;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * Custom RequestMappingHandlerMapping to support API versioning.
 */
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

        if (info == null) {
            return null;
        }

        // Check for @ApiVersion annotation on method
        ApiVersion methodAnnotation = AnnotationUtils.findAnnotation(method, ApiVersion.class);

        // Check for @ApiVersion annotation on class
        ApiVersion classAnnotation = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);

        // Use method annotation if present, otherwise use class annotation
        ApiVersion apiVersion = methodAnnotation != null ? methodAnnotation : classAnnotation;

        if (apiVersion != null) {
            RequestCondition<?> condition = new ApiVersionRequestCondition(apiVersion.value());
            return new RequestMappingInfo(
                    info.getName(),
                    info.getPatternsCondition().getPatterns().stream()
                            .map(pattern -> "/api/v" + apiVersion.value() + pattern)
                            .collect(java.util.stream.Collectors.toSet()),
                    info.getPathPatternsCondition(),
                    info.getMethodsCondition(),
                    info.getParamsCondition(),
                    info.getHeadersCondition(),
                    info.getConsumesCondition(),
                    info.getProducesCondition(),
                    condition
            );
        }

        return info;
    }
}
