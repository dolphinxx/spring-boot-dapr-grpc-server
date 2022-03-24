package com.github.dolphinxx.grpc;

import com.google.protobuf.AbstractMessage;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Map;

public class GrpcMappingCollector {
    public static void collect(Object obj, Map<String, GrpcMappingMethodInfo> result) {
        GrpcMapping clazzAnnotation = AnnotationUtils.getAnnotation(obj.getClass(), GrpcMapping.class);
        if (clazzAnnotation == null) {
            return;
        }
        String prefix = clazzAnnotation.value();
        if (prefix.equals("/")) {
            prefix = "";
        }
        for (Method method : obj.getClass().getDeclaredMethods()) {
            GrpcMapping annotation = AnnotationUtils.getAnnotation(method, GrpcMapping.class);
            if (annotation != null) {
                String path = annotation.value();
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new RuntimeException("Method annotated with @GrpcMapping should declare a single parameter of type AbstractMessage or a subclass.");
                }
                //noinspection unchecked
                result.put(joinPath(prefix, path), new GrpcMappingMethodInfo(method, obj, (Class<? extends AbstractMessage>) parameterTypes[0]));
            }
        }
    }

    public static String joinPath(String prefix, String path) {
        StringBuilder result = new StringBuilder();
        if (!path.startsWith("/")) {
            result.append(prefix).append("/").append(path);
        } else {
            result.append(prefix).append(path);
        }
        if (result.length() > 0 && result.charAt(0) != '/') {
            return "/" + result;
        }
        return result.toString();
    }
}
