package com.github.dolphinxx.grpc;

import com.google.protobuf.Any;
import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.CommonProtos;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = "grpc.register-raw=true")
public class GrpcMappingTest {
    @GrpcMapping
    public static class TestGrpcHandler extends GreeterGrpc.GreeterImplBase {
        /**
         * For requests without dapr
         */
        @Override
        public void greeting(GreetingProtos.GreetingRequest request, StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
            try {
                responseObserver.onNext(greeting(request));
            } finally {
                responseObserver.onCompleted();
            }
        }

        @GrpcMapping("/greeting")
        public GreetingProtos.GreetingResponse greeting(GreetingProtos.GreetingRequest request) {
            String name = request.getName();
            return GreetingProtos.GreetingResponse.newBuilder().setMessage("Hello " + name).build();
        }
    }

    @SpringBootConfiguration
    @Import(DaprGrpcConfiguration.class)
    public static class TestConfig {
        @Bean
        public TestGrpcHandler testMapping() {
            return new TestGrpcHandler();
        }
    }

    @Test
    public void testGrpcMapping() throws Exception {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget("localhost:5000")
                .usePlaintext()
                .build();
        AppCallbackGrpc.AppCallbackBlockingStub blockingStub = AppCallbackGrpc.newBlockingStub(channel);
        GreetingProtos.GreetingRequest greetingRequest = GreetingProtos.GreetingRequest.newBuilder().setName("Dapr").build();
        CommonProtos.InvokeRequest request = CommonProtos.InvokeRequest
                .newBuilder()
                .setData(Any.pack(greetingRequest))
                .setMethod("/greeting")
                .build();
        CommonProtos.InvokeResponse response = blockingStub.onInvoke(request);
        String result = response.getData().unpack(GreetingProtos.GreetingResponse.class).getMessage();
        System.out.println();
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        Assertions.assertEquals("Hello Dapr", result);
    }

    @Test
    public void testRegisterRaw() throws Exception {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget("localhost:5000")
                .usePlaintext()
                .build();
        GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);
        GreetingProtos.GreetingRequest request = GreetingProtos.GreetingRequest.newBuilder().setName("Dapr").build();
        GreetingProtos.GreetingResponse response = blockingStub.greeting(request);
        String result = response.getMessage();
        System.out.println();
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        Assertions.assertEquals("Hello Dapr", result);
    }
}
