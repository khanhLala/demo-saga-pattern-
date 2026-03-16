package com.example.order.service;

import com.example.order.dto.InventoryRequest;
import com.example.order.dto.InventoryResponse;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.PaymentRequest;
import com.example.order.dto.PaymentResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    OrderRepository orderRepository;
    RestTemplate restTemplate;

    String INVENTORY_URL = "http://localhost:8082/inventory";
    String PAYMENT_URL = "http://localhost:8083/payment";

    public Order createOrder(OrderRequest request) {
        // 1. Save order PENDING
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setTransactionId(UUID.randomUUID().toString());
        orderRepository.save(order);

        log.info("[ORCHESTRATOR] Da tao order | txId={} | status=PENDING", order.getTransactionId());

        try {
            // 2. Call Inventory to Reserve
            log.info("[ORCHESTRATOR] Dang goi InventoryService.reserve | txId={}", order.getTransactionId());
            InventoryRequest invReq = new InventoryRequest(order.getTransactionId(), order.getProductId(), order.getQuantity());
            InventoryResponse invRes = restTemplate.postForObject(INVENTORY_URL + "/reserve", invReq, InventoryResponse.class);

            if (invRes != null && invRes.isSuccess()) {
                log.info("[ORCHESTRATOR] InventoryReserved thanh cong | txId={}", order.getTransactionId());

                // 3. Call Payment to Charge
                log.info("[ORCHESTRATOR] Dang goi PaymentService.charge | txId={}", order.getTransactionId());
                PaymentRequest payReq = new PaymentRequest(order.getTransactionId(), order.getQuantity());
                PaymentResponse payRes = restTemplate.postForObject(PAYMENT_URL + "/charge", payReq, PaymentResponse.class);

                if (payRes != null && payRes.isSuccess()) {
                    // 4. Success -> Update Order COMPLETED
                    log.info("[ORCHESTRATOR] Payment charge thanh cong | txId={}", order.getTransactionId());
                    order.setStatus(OrderStatus.COMPLETED);
                } else {
                    // 5a. Payment failed -> Rollback Inventory and Update Order FAILED
                    log.warn("[ORCHESTRATOR] Payment charge THAT BAI | txId={} | reason={}", 
                            order.getTransactionId(), payRes != null ? payRes.getMessage() : "null");
                    rollbackInventory(order.getTransactionId());
                    order.setStatus(OrderStatus.FAILED);
                }
            } else {
                // 5b. Inventory failed -> Update Order FAILED
                log.warn("[ORCHESTRATOR] Inventory reserve THAT BAI | txId={} | reason={}", 
                        order.getTransactionId(), invRes != null ? invRes.getMessage() : "null");
                order.setStatus(OrderStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("[ORCHESTRATOR] Loi trong qua trinh orchestration | txId={} | logic={}", order.getTransactionId(), e.getMessage());
            order.setStatus(OrderStatus.FAILED);
        }

        orderRepository.save(order);
        log.info("[ORCHESTRATOR] Hoan tat workflow | txId={} | status={}", order.getTransactionId(), order.getStatus());
        return order;
    }

    private void rollbackInventory(String transactionId) {
        try {
            log.info("[ORCHESTRATOR] Dang thuc hien compensation: Refund Inventory | txId={}", transactionId);
            InventoryRequest invReq = new InventoryRequest();
            invReq.setTransactionId(transactionId);
            restTemplate.postForObject(INVENTORY_URL + "/refund", invReq, Void.class);
        } catch (Exception e) {
            log.error("[ORCHESTRATOR] Loi khi rollback inventory | txId={} | error={}", transactionId, e.getMessage());
        }
    }
}
