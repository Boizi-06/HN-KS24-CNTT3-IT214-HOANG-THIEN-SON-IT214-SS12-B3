package com.storex.payment.controller;

import com.storex.payment.dto.PaymentRequest;
import com.storex.payment.dto.PaymentResponse;
import com.storex.payment.service.BankPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private BankPaymentService bankPaymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = bankPaymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}
