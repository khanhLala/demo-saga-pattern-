package com.example.order.service;

import com.example.order.dto.OrderRequest;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.event.InventoryFailedEvent;
import com.example.order.event.OrderCreatedEvent;
import com.example.order.event.PaymentBilledEvent;
import com.example.order.event.PaymentFailedEvent;
import com.example.order.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    OrderRepository orderRepository;
    KafkaTemplate<String, Object> kafkaTemplate;

    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setTransactionId(UUID.randomUUID().toString());
        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getTransactionId(),
                order.getProductId(),
                order.getQuantity()
        );

        log.info("[ORDER] Da tao order | txId={} | productId={} | quantity={} | status=PENDING",
                order.getTransactionId(), order.getProductId(), order.getQuantity());
        log.info("[ORDER] Da gui su kien OrderCreatedEvent -> topic: order-events");

        kafkaTemplate.send("order-events", event);
        return order;
    }

    /** Nhan ket qua tu payment (thanh cong hoac that bai) */
    @KafkaListener(topics = "payment-events", groupId = "order-group")
    public void handlePaymentEvents(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        if (event instanceof PaymentBilledEvent billedEvent) {
            log.info("[ORDER] Da nhan su kien PaymentBilledEvent | txId={}", billedEvent.getTransactionId());
            orderRepository.findByTransactionId(billedEvent.getTransactionId()).ifPresent(order -> {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                log.info("[ORDER] Cap nhat trang thai order thanh COMPLETED | txId={}", billedEvent.getTransactionId());
            });

        } else if (event instanceof PaymentFailedEvent failedEvent) {
            log.info("[ORDER] Da nhan su kien PaymentFailedEvent | txId={} | reason={}", failedEvent.getTransactionId(), failedEvent.getReason());
            orderRepository.findByTransactionId(failedEvent.getTransactionId()).ifPresent(order -> {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                log.info("[ORDER] Cap nhat trang thai order thanh FAILED (thanh toan that bai) | txId={}", failedEvent.getTransactionId());
            });

        } else {
            log.warn("[ORDER] Kieu event khong xac dinh tu payment-events: {}", event == null ? "null" : event.getClass().getName());
        }
    }

    /** Nhan thong bao kho khong du hang tu inventory */
    @KafkaListener(topics = "inventory-events", groupId = "order-inventory-group")
    public void handleInventoryEvents(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        if (event instanceof InventoryFailedEvent failedEvent) {
            log.info("[ORDER] Da nhan su kien InventoryFailedEvent | txId={} | reason={}", failedEvent.getTransactionId(), failedEvent.getReason());
            orderRepository.findByTransactionId(failedEvent.getTransactionId()).ifPresent(order -> {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                log.info("[ORDER] Cap nhat trang thai order thanh FAILED (kho khong du) | txId={}", failedEvent.getTransactionId());
            });
        } else {
            log.warn("[ORDER] Kieu event khong can xu ly tu inventory-events: {}", event == null ? "null" : event.getClass().getSimpleName());
        }
    }
}
