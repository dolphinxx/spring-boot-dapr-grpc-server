package com.github.dolphinxx.grpc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GrpcMappingMethodInfo {
    private final Method method;
    private final Object target;
    private final Class<? extends AbstractMessage> messageType;

    public GrpcMappingMethodInfo(Method method, Object target, Class<? extends AbstractMessage> messageType) {
        this.method = method;
        this.target = target;
        this.messageType = messageType;
    }

    public <T extends AbstractMessage> T invoke(Any message) {
        try {
            //noinspection unchecked
            return (T) method.invoke(target, message.unpack(messageType));
        } catch (InvocationTargetException | IllegalAccessException | InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
