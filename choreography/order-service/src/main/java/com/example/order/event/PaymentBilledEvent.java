package com.example.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBilledEvent {
    private String transactionId;
    private String status; // e.g. "SUCCESS", "FAILED"
}
