package com.github.dolphinxx.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc")
public class DaprGrpcProperties {
    private int port;
    private boolean registerRaw;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isRegisterRaw() {
        return registerRaw;
    }

    public void setRegisterRaw(boolean registerRaw) {
        this.registerRaw = registerRaw;
    }
}
