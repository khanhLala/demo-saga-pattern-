package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentResponse charge(PaymentRequest request) {
        log.info("Nhan yeu cau charge | txId={} | quantity={}",
                request.getTransactionId(), request.getQuantity());

        boolean isSuccess = request.getQuantity() != 999;
        long amount = 100L * request.getQuantity();

        Payment payment = new Payment();
        payment.setTransactionId(request.getTransactionId());
        payment.setAmount(amount);

        if (isSuccess) {
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);
            log.info("Thanh toan thanh cong | amount={} VND | txId={}", amount, request.getTransactionId());
            return new PaymentResponse(request.getTransactionId(), true, "Payment success");
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            log.info("Thanh toan that bai | txId={}", request.getTransactionId());
            return new PaymentResponse(request.getTransactionId(), false, "Insufficient funds demo");
        }
    }
}
