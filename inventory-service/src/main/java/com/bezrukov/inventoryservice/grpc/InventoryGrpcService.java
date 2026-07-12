package com.bezrukov.inventoryservice.grpc;

import com.bezrukov.common.grpc.InventoryServiceGrpc;
import com.bezrukov.common.grpc.ProductResponse;
import com.bezrukov.common.grpc.ProductRequest;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.service.ProductService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final ProductService productService;

    @Override
    public void checkAvailability(
            ProductRequest request,
            StreamObserver<ProductResponse> responseObserver) {
        try {
            Long productId = request.getProductId();
            Long quantity = request.getQuantity();

            log.info("gRPC call: checkAvailability for product {}, quantity {}",
                    productId, quantity);

            Product product = productService.getProductWithAvailability(productId, quantity);

            ProductResponse response = ProductResponse.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setQuantity(product.getQuantity())
                    .setPrice(product.getPrice().doubleValue())
                    .setSalePercent(product.getSalePercent() != null ? product.getSalePercent() : 0)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC response sent: productId={}", productId);

        } catch (Exception e) {
            log.error("gRPC error: {}", e.getMessage());

            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Product not found or insufficient quantity")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
