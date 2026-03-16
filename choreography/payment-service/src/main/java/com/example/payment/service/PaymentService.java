package com.example.payment.service;

import com.example.payment.entity.Payment;
import com.example.payment.event.InventoryReservedEvent;
import com.example.payment.event.PaymentBilledEvent;
import com.example.payment.event.PaymentFailedEvent;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "inventory-events", groupId = "payment-group")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("[PAYMENT] Da nhan su kien InventoryReservedEvent | txId={} | quantity={}",
                event.getTransactionId(), event.getQuantity());

        boolean isSuccess = event.getQuantity() != 999;
        long amount = 100L * event.getQuantity();

        Payment payment = new Payment();
        payment.setTransactionId(event.getTransactionId());
        payment.setAmount(amount);

        if (isSuccess) {
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);
            log.info("[PAYMENT] Thanh toan thanh cong | amount={} VND | txId={}", amount, event.getTransactionId());

            PaymentBilledEvent billedEvent = new PaymentBilledEvent(event.getTransactionId(), "SUCCESS");
            log.info("[PAYMENT] Da gui su kien PaymentBilledEvent -> topic: payment-events | txId={}", event.getTransactionId());
            kafkaTemplate.send("payment-events", billedEvent);
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            log.info("[PAYMENT] Thanh toan that bai | txId={}", event.getTransactionId());

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(event.getTransactionId(), "Insufficient funds demo");
            log.info("[PAYMENT] Da gui su kien PaymentFailedEvent -> topic: payment-events | txId={}", event.getTransactionId());
            kafkaTemplate.send("payment-events", failedEvent);
        }
    }
}
