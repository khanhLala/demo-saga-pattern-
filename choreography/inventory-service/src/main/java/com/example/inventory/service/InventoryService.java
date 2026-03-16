package com.example.inventory.service;

import com.example.inventory.event.InventoryFailedEvent;
import com.example.inventory.event.InventoryReservedEvent;
import com.example.inventory.event.OrderCreatedEvent;
import com.example.inventory.event.PaymentFailedEvent;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Map<String, Integer> reservedQuantityMap = new HashMap<>();

    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[INVENTORY] Da nhan su kien OrderCreatedEvent | txId={} | productId={} | quantity={}",
                event.getTransactionId(), event.getProductId(), event.getQuantity());

        inventoryRepository.findByProductId(event.getProductId()).ifPresentOrElse(inv -> {
            if (inv.getAvailableQuantity() >= event.getQuantity()) {
                inv.setAvailableQuantity(inv.getAvailableQuantity() - event.getQuantity());
                inventoryRepository.save(inv);
                reservedQuantityMap.put(event.getTransactionId(), event.getQuantity());

                log.info("[INVENTORY] Da tru kho | quantity={} | con lai={} | txId={}",
                        event.getQuantity(), inv.getAvailableQuantity(), event.getTransactionId());

                InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                        event.getTransactionId(), event.getProductId(), event.getQuantity()
                );
                log.info("[INVENTORY] Da gui su kien InventoryReservedEvent -> topic: inventory-events | txId={}", event.getTransactionId());
                kafkaTemplate.send("inventory-events", reservedEvent);
            } else {
                log.warn("[INVENTORY] Khong du hang | can={} | co={} | txId={}",
                        event.getQuantity(), inv.getAvailableQuantity(), event.getTransactionId());

                InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                        event.getTransactionId(), event.getProductId(), event.getQuantity(), "Khong du hang trong kho"
                );
                log.info("[INVENTORY] Da gui su kien InventoryFailedEvent -> topic: inventory-events | txId={}", event.getTransactionId());
                kafkaTemplate.send("inventory-events", failedEvent);
            }
        }, () -> {
            log.error("[INVENTORY] Khong tim thay san pham productId={}", event.getProductId());
            InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                    event.getTransactionId(), event.getProductId(), event.getQuantity(), "Khong tim thay san pham"
            );
            kafkaTemplate.send("inventory-events", failedEvent);
        });
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-group-failed")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("[INVENTORY] Da nhan su kien PaymentFailedEvent | txId={}", event.getTransactionId());

        Integer reversedQuantity = reservedQuantityMap.remove(event.getTransactionId());
        if (reversedQuantity != null) {
            inventoryRepository.findByProductId(1L).ifPresent(inv -> {
                inv.setAvailableQuantity(inv.getAvailableQuantity() + reversedQuantity);
                inventoryRepository.save(inv);
                log.info("[INVENTORY] Da hoan kho | +{} san pham | tong con lai={} | txId={}",
                        reversedQuantity, inv.getAvailableQuantity(), event.getTransactionId());
            });
        } else {
            log.warn("[INVENTORY] Khong tim thay du lieu dat cho txId={}, co the da xu ly truoc do", event.getTransactionId());
        }
    }
}
