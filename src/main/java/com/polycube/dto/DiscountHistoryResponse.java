package com.polycube.dto;

import com.polycube.domain.DiscountHistory;
import com.polycube.domain.enums.MemberGrade;
import com.polycube.domain.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 할인 이력 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountHistoryResponse {

    private Long id;
    private String policyType;
    private String policyName;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private MemberGrade memberGradeAtPayment;
    private PaymentMethod paymentMethod;
    private Integer applyOrder;
    private BigDecimal baseAmount;
    private LocalDateTime createdAt;

    public static DiscountHistoryResponse from(DiscountHistory discountHistory) {
        return DiscountHistoryResponse.builder()
                .id(discountHistory.getId())
                .policyType(discountHistory.getPolicyType())
                .policyName(discountHistory.getPolicyName())
                .discountRate(discountHistory.getDiscountRate())
                .discountAmount(discountHistory.getDiscountAmount())
                .memberGradeAtPayment(discountHistory.getMemberGradeAtPayment())
                .paymentMethod(discountHistory.getPaymentMethod())
                .applyOrder(discountHistory.getApplyOrder())
                .baseAmount(discountHistory.getBaseAmount())
                .createdAt(discountHistory.getCreatedAt())
                .build();
    }
}
