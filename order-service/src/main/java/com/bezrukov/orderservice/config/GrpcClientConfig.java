package com.bezrukov.orderservice.config;

import com.bezrukov.common.grpc.InventoryServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Slf4j
@Configuration
@ImportGrpcClients(target = "inventory-service", types = InventoryServiceGrpc.InventoryServiceBlockingStub.class)
public class GrpcClientConfig {
}
