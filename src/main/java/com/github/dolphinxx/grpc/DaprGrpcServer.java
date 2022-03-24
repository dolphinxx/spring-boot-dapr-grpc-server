package com.github.dolphinxx.grpc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.CommonProtos;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DaprGrpcServer extends AppCallbackGrpc.AppCallbackImplBase implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(DaprGrpcServer.class);
    private Map<String, GrpcMappingMethodInfo> mappings;
    private ApplicationContext applicationContext;
    private final int port;
    private final boolean registerRaw;
    private Server server;

    public DaprGrpcServer(int port, boolean registerRaw) {
        this.port = port <= 0 ? 5000 : port;
        this.registerRaw = registerRaw;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStarted() throws IOException {
        if (server != null) {
            server.shutdownNow();
        }
        Map<String, Object> mappers = applicationContext.getBeansWithAnnotation(GrpcMapping.class);
        if (mappers.isEmpty()) {
            return;
        }
        Map<String, GrpcMappingMethodInfo> mappings = new HashMap<>(mappers.size() * 4);
        for (var mapper : mappers.values()) {
            GrpcMappingCollector.collect(mapper, mappings);
        }
        this.mappings = mappings;
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port)
                .addService(this);
        if (registerRaw) {
            Collection<BindableService> services = applicationContext.getBeansOfType(BindableService.class).values();
            if (!services.isEmpty()) {
                serverBuilder.addServices(services.stream().map(BindableService::bindService).collect(Collectors.toUnmodifiableList()));
            }
        }
        server = serverBuilder
                .build()
                .start();
        logger.info("GRPC server started, listening on " + port);
    }

    @EventListener(ContextClosedEvent.class)
    public void onApplicationStopped() {
        if (server != null) {
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                logger.info("GRPC server stopped.");
            } catch (InterruptedException e) {
                logger.error("Error occurred while shutdown GRPC server.", e);
            }
        }
    }

    @Override
    public void onInvoke(CommonProtos.InvokeRequest request, StreamObserver<CommonProtos.InvokeResponse> responseObserver) {
        String method = request.getMethod();
        GrpcMappingMethodInfo methodInfo = mappings.get(method);
        if (methodInfo == null) {
            throw new AssertionError();
        }
        try {
            AbstractMessage result = methodInfo.invoke(request.getData());
            responseObserver.onNext(CommonProtos.InvokeResponse.newBuilder().setData(Any.pack(result)).build());
        } finally {
            responseObserver.onCompleted();
        }
    }
}
