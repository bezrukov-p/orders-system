package com.bezrukov.inventoryservice.integrations;

import com.bezrukov.common.dto.OrderItemDto;
import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.inventoryservice.TestcontainersConfiguration;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.repository.IdempotencyKeyRepository;
import com.bezrukov.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderCommandConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
    }
    @Test
    @DisplayName("При достаточном количестве товар резервируется")
    void shouldReserveStockSuccessfully() {
        Product product = productRepository.save(Product.builder()
                .name("iPhone")
                .quantity(100L)
                .price(999.99)
                .salePercent(0)
                .build());

        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(UUID.randomUUID())
                .idempotencyKey("key-" + UUID.randomUUID())
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(product.getId())
                                .quantity(5L)
                                .build()
                ))
                .build();
        kafkaTemplate.send("order-commands", command.getOrderId().toString(), command);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Product updated = productRepository.findById(product.getId()).orElseThrow();
                    assertThat(updated.getQuantity()).isEqualTo(95L);
                });
    }
    @Test
    @DisplayName("При недостатке товара резервирование не происходит")
    void shouldNotReserveWhenInsufficientStock() {
        Product product = productRepository.save(Product.builder()
                .name("iPhone")
                .quantity(3L)
                .price(999.99)
                .salePercent(0)
                .build());

        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(UUID.randomUUID())
                .idempotencyKey("key-" + UUID.randomUUID())
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(product.getId())
                                .quantity(10L)
                                .build()
                ))
                .build();
        kafkaTemplate.send("order-commands", command.getOrderId().toString(), command);

        await().during(2, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Product updated = productRepository.findById(product.getId()).orElseThrow();
                    assertThat(updated.getQuantity()).isEqualTo(3L);
                });
    }

    @Test
    @DisplayName("Повторная команда с тем же idempotencyKey игнорируется")
    void shouldIgnoreDuplicateCommand() {
        Product product = productRepository.save(Product.builder()
                .name("iPhone")
                .quantity(100L)
                .price(999.99)
                .salePercent(0)
                .build());

        String idempotencyKey = "same-key-" + UUID.randomUUID();
        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(product.getId())
                                .quantity(5L)
                                .build()
                ))
                .build();

        kafkaTemplate.send("order-commands", command.getOrderId().toString(), command);
        kafkaTemplate.send("order-commands", command.getOrderId().toString(), command);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Product updated = productRepository.findById(product.getId()).orElseThrow();
                    assertThat(updated.getQuantity()).isEqualTo(95L);
                });

        assertThat(idempotencyKeyRepository.existsByKey(idempotencyKey)).isTrue();
    }

    @Test
    @DisplayName("Резервирование нескольких товаров в одном заказе")
    void shouldReserveMultipleProducts() {
        Product product1 = productRepository.save(Product.builder()
                .name("iPhone")
                .quantity(100L)
                .price(999.99)
                .salePercent(0)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("MacBook")
                .quantity(50L)
                .price(1999.99)
                .salePercent(10)
                .build());

        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(UUID.randomUUID())
                .idempotencyKey("key-" + UUID.randomUUID())
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(product1.getId())
                                .quantity(2L)
                                .build(),
                        OrderItemDto.builder()
                                .productId(product2.getId())
                                .quantity(1L)
                                .build()
                ))
                .build();
        kafkaTemplate.send("order-commands", command.getOrderId().toString(), command);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Product updated1 = productRepository.findById(product1.getId()).orElseThrow();
                    Product updated2 = productRepository.findById(product2.getId()).orElseThrow();
                    assertThat(updated1.getQuantity()).isEqualTo(98L);
                    assertThat(updated2.getQuantity()).isEqualTo(49L);
                });
    }
}
