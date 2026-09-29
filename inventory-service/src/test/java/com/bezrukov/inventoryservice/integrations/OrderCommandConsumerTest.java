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

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderCommandConsumerTest {

    private static final String TOPIC = "order-commands";
    private static final String IPHONE_NAME = "iPhone";
    private static final String MACBOOK_NAME = "MacBook";

    private static final double IPHONE_PRICE = 999.99;
    private static final double MACBOOK_PRICE = 1999.99;

    private static final Duration KAFKA_SEND_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_POLL = Duration.ofMillis(100);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @AfterEach
    void tearDown() {
        idempotencyKeyRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("При достаточном количестве товар резервируется")
    void shouldReserveStockSuccessfully() {
        Product iphone = saveProduct(IPHONE_NAME, 100L, IPHONE_PRICE, 0);
        long reservedQuantity = 5L;
        long expectedQuantity = 100L - reservedQuantity;

        sendReserveStockCommand(iphone.getId(), reservedQuantity);

        awaitQuantity(iphone.getId(), expectedQuantity);
    }
    @Test
    @DisplayName("При недостатке товара резервирование не происходит")
    void shouldNotReserveWhenInsufficientStock() {
        Product iphone = saveProduct(IPHONE_NAME, 3L, IPHONE_PRICE, 0);
        long initialQuantity = 3L;
        long requestedQuantity = 10L;

        sendReserveStockCommand(iphone.getId(), requestedQuantity);

        awaitQuantity(iphone.getId(), initialQuantity);
    }

    @Test
    @DisplayName("Повторная команда с тем же idempotencyKey игнорируется")
    void shouldIgnoreDuplicateCommand() {
        Product iphone = saveProduct(IPHONE_NAME, 100L, IPHONE_PRICE, 0);
        String idempotencyKey = randomIdempotencyKey();
        long reservedQuantity = 5L;
        long expectedQuantity = 100L - reservedQuantity;

        sendReserveStockCommand(iphone.getId(), reservedQuantity, idempotencyKey);
        sendReserveStockCommand(iphone.getId(), reservedQuantity, idempotencyKey);

        awaitQuantity(iphone.getId(), expectedQuantity);
        assertThat(idempotencyKeyRepository.existsByKey(idempotencyKey)).isTrue();
    }

    @Test
    @DisplayName("Резервирование нескольких товаров в одном заказе")
    void shouldReserveMultipleProducts() {
        Product iphone = saveProduct(IPHONE_NAME, 100L, IPHONE_PRICE, 0);
        Product macbook = saveProduct(MACBOOK_NAME, 50L, MACBOOK_PRICE, 10);

        long iphoneReserved = 2L;
        long macbookReserved = 1L;
        long iphoneExpected = 100L - iphoneReserved;
        long macbookExpected = 50L - macbookReserved;

        sendReserveStockCommand(List.of(
                item(iphone.getId(), iphoneReserved),
                item(macbook.getId(), macbookReserved)
        ));

        awaitQuantity(iphone.getId(), iphoneExpected);
        awaitQuantity(macbook.getId(), macbookExpected);
    }

    private Product saveProduct(String name, long quantity, double price, int salePercent) {
        return productRepository.save(Product.builder()
                .name(name)
                .quantity(quantity)
                .price(price)
                .salePercent(salePercent)
                .build());
    }

    private OrderItemDto item(long productId, long quantity) {
        return OrderItemDto.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private String randomIdempotencyKey() {
        return "key-" + UUID.randomUUID();
    }

    private void sendReserveStockCommand(long productId, long quantity) {
        sendReserveStockCommand(productId, quantity, randomIdempotencyKey());
    }

    private void sendReserveStockCommand(long productId, long quantity, String idempotencyKey) {
        sendReserveStockCommand(
                List.of(item(productId, quantity)),
                idempotencyKey
        );
    }

    private void sendReserveStockCommand(List<OrderItemDto> items) {
        sendReserveStockCommand(items, randomIdempotencyKey());
    }

    private void sendReserveStockCommand(List<OrderItemDto> items, String idempotencyKey) {
        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .items(items)
                .build();

        kafkaTemplate.send(TOPIC, command.getOrderId().toString(), command)
                .orTimeout(KAFKA_SEND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .join();
    }

    private void awaitQuantity(long productId, long expected) {
        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .untilAsserted(() ->
                        assertThat(quantityOf(productId)).isEqualTo(expected)
                );
    }

    private long quantityOf(long productId) {
        return productRepository.findById(productId).orElseThrow().getQuantity();
    }
}
