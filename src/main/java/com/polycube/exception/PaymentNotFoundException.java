package com.polycube.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long id) {
        super("결제를 찾을 수 없습니다. ID: " + id);
    }
}
