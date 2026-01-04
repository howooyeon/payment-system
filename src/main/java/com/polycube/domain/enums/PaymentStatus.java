package com.polycube.domain.enums;

public enum PaymentStatus {
    PENDING,    // 결제 대기
    APPROVED,   // 결제 승인
    CANCELLED,  // 결제 취소
    REFUNDED    // 환불 완료
}
