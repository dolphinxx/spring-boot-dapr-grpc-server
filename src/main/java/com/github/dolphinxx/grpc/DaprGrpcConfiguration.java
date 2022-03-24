package com.github.dolphinxx.grpc;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(DaprGrpcProperties.class)
public class DaprGrpcConfiguration {
    @Bean
    public DaprGrpcServer grpcServer(DaprGrpcProperties properties) {
        return new DaprGrpcServer(properties.getPort(), properties.isRegisterRaw());
    }
}
