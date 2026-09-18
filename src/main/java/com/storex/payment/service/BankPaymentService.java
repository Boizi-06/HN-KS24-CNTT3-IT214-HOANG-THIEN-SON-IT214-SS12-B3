package com.storex.payment.service;

import com.storex.payment.dto.PaymentRequest;
import com.storex.payment.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BankPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BankPaymentService.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Gọi API thanh toán sang Ngân hàng đối tác.
     * Áp dụng Circuit Breaker với tên instance là 'bankClient'.
     */
    @CircuitBreaker(name = "bankClient", fallbackMethod = "fallbackPayment")
    public PaymentResponse processPayment(PaymentRequest request) {
        String bankApiUrl = "https://partner-bank.com/api/v1/charge";
        // Gọi sang API Ngân hàng đối tác
        return restTemplate.postForObject(bankApiUrl, request, PaymentResponse.class);
    }

    /**
     * Phương thức Fallback khi Ngân hàng đối tác lỗi hoặc Cầu dao đang Mở mạch.
     */
    public PaymentResponse fallbackPayment(PaymentRequest request, Throwable throwable) {
        if (throwable instanceof CallNotPermittedException) {
            log.warn("[CIRCUIT BREAKER OPEN] Cầu dao 'bankClient' đang MỞ MẠCH. Tạm dừng kết nối 30s theo yêu cầu của Ngân hàng đối tác.");
            return new PaymentResponse(
                "FAILED",
                "Hệ thống ngân hàng đối tác đang bảo trì (Cầu dao ngắt 30s). Vui lòng thử lại sau.",
                null
            );
        }

        log.error("[BANK API ERROR] Gọi cổng thanh toán ngân hàng thất bại: {}", throwable.getMessage());
        return new PaymentResponse(
            "FAILED",
            "Không thể kết nối tới cổng thanh toán ngân hàng. Vui lòng thử lại sau.",
            null
        );
    }
}
