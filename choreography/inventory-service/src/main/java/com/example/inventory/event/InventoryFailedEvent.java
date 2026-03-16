package com.example.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {
    private String transactionId;
    private Long productId;
    private Integer quantity;
    private String reason;
}
