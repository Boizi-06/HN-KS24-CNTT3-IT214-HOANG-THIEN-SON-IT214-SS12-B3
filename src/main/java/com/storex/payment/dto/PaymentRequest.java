package com.storex.payment.dto;

public class PaymentRequest {
    private String orderId;
    private Double amount;
    private String bankCode;

    public PaymentRequest() {
    }

    public PaymentRequest(String orderId, Double amount, String bankCode) {
        this.orderId = orderId;
        this.amount = amount;
        this.bankCode = bankCode;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
}
