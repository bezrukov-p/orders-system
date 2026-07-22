package com.bezrukov.inventoryservice.grpc;

import com.bezrukov.common.grpc.InventoryServiceGrpc;
import com.bezrukov.common.grpc.ProductResponse;
import com.bezrukov.common.grpc.ProductRequest;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.exception.ProductNotFoundException;
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
            StreamObserver<ProductResponse> responseObserver
    ) {
        Long productId = request.getProductId();
        Long quantity = request.getQuantity();
        try {

            log.info("check availability: productId={}, requestedQuantity={}",
                    productId, quantity);

            if (productId <= 0) {
                log.warn("incorrect productId: {}", productId);
                responseObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription("Product ID must be positive")
                                .asRuntimeException()
                );
                return;
            }

            if (quantity <= 0) {
                log.warn("incorrect requested quantity: {}", quantity);
                responseObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription("Quantity must be positive")
                                .asRuntimeException()
                );
                return;
            }

            Product product = productService.getProductWithAvailability(productId, quantity);

            ProductResponse response = ProductResponse.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setQuantity(product.getQuantity())
                    .setPrice(product.getPrice())
                    .setSalePercent(product.getSalePercent() != null ? product.getSalePercent() : 0)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("product available: productId={}, available={}, requested={}",
                    productId, product.getQuantity(), quantity);

        } catch (ProductNotFoundException e) {
            log.warn("product not found: productId={}, error={}",
                    productId, e.getMessage());

            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Product not found: " + productId)
                            .withCause(e)
                            .asRuntimeException()
            );

        } catch (IllegalStateException e) {
            log.warn("product unavailable: productId={}, requested={}, error={}",
                    productId, quantity, e.getMessage());

            responseObserver.onError(
                    Status.RESOURCE_EXHAUSTED
                            .withDescription("Insufficient quantity available")
                            .withCause(e)
                            .asRuntimeException()
            );

        } catch (Exception e) {
            log.error("Unexpected error when checking the product: productId={}, error={}",
                    productId, e.getMessage(), e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
