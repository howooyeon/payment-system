package com.polycube.exception;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(Long orderId) {
        super("주문 ID " + orderId + "에 대한 승인된 결제가 이미 존재합니다.");
    }
}
