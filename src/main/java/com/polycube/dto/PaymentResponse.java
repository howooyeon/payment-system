package com.polycube.dto;

import com.polycube.domain.Payment;
import com.polycube.domain.enums.PaymentMethod;
import com.polycube.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DiscountHistoryResponse> discountHistories;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .amount(payment.getAmount())
                .discountAmount(payment.getDiscountAmount())
                .finalAmount(payment.getFinalAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .discountHistories(payment.getDiscountHistories() != null
                        ? payment.getDiscountHistories().stream()
                                .map(DiscountHistoryResponse::from)
                                .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
