package com.example.inventory.service;

import com.example.inventory.dto.InventoryRequest;
import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final Map<String, Integer> reservedQuantityMap = new HashMap<>();

    public InventoryResponse reserve(InventoryRequest request) {
        log.info("Nhan yeu cau reserve | txId={} | productId={} | quantity={}",
                request.getTransactionId(), request.getProductId(), request.getQuantity());

        return inventoryRepository.findByProductId(request.getProductId()).map(inv -> {
            if (inv.getAvailableQuantity() >= request.getQuantity()) {
                inv.setAvailableQuantity(inv.getAvailableQuantity() - request.getQuantity());
                inventoryRepository.save(inv);
                reservedQuantityMap.put(request.getTransactionId(), request.getQuantity());

                log.info("Da tru kho | quantity={} | con lai={} | txId={}",
                        request.getQuantity(), inv.getAvailableQuantity(), request.getTransactionId());
                return new InventoryResponse(request.getTransactionId(), true, "Reserved successfully");
            } else {
                log.warn("Khong du hang | can={} | co={} | txId={}",
                        request.getQuantity(), inv.getAvailableQuantity(), request.getTransactionId());
                return new InventoryResponse(request.getTransactionId(), false, "Khong du hang trong kho");
            }
        }).orElseGet(() -> {
            log.error("Khong tim thay san pham productId={}", request.getProductId());
            return new InventoryResponse(request.getTransactionId(), false, "Khong tim thay san pham");
        });
    }

    public void refund(String transactionId) {
        log.info("Nhan yeu cau refund | txId={}", transactionId);

        Integer reversedQuantity = reservedQuantityMap.remove(transactionId);
        if (reversedQuantity != null) {
            inventoryRepository.findByProductId(1L).ifPresent(inv -> {
                inv.setAvailableQuantity(inv.getAvailableQuantity() + reversedQuantity);
                inventoryRepository.save(inv);
                log.info("Da hoan kho | +{} san pham | tong con lai={} | txId={}",
                        reversedQuantity, inv.getAvailableQuantity(), transactionId);
            });
        } else {
            log.warn("Khong tim thay du lieu dat cho txId={}, co the da xu ly truoc do", transactionId);
        }
    }
}
