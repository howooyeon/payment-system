package com.polycube.domain;

import com.polycube.domain.enums.MemberGrade;
import com.polycube.domain.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiscountHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false) // 연관된 결제
    private Payment payment;

    @Column(nullable = false, length = 50)
    private String policyType;

    /**
     * 할인 정책명 (예: "VIP 1000원 할인", "포인트 결제 5% 추가 할인")
     */
    @Column(nullable = false, length = 200)
    private String policyName;

    /**
     * 할인율 (퍼센트, 예: 10% 할인 시 10.00)
     * 고정 금액 할인의 경우 null 가능
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    /**
     * 결제 시점의 회원 등급 (정책 변경 후에도 당시 등급 보존)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberGrade memberGradeAtPayment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /**
     * 할인 적용 순서 (1: 첫 번째 할인, 2: 두 번째 할인 등)
     */
    @Column(nullable = false)
    private Integer applyOrder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 할인 이력 생성 팩토리 메서드 -> 등급 할인용
     */
    public static DiscountHistory createGradeDiscount(
            Payment payment,
            String policyName,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            MemberGrade memberGrade,
            Integer applyOrder,
            BigDecimal baseAmount
    ) {
        return DiscountHistory.builder()
                .payment(payment)
                .policyType("GRADE_DISCOUNT")
                .policyName(policyName)
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .memberGradeAtPayment(memberGrade)
                .applyOrder(applyOrder)
                .baseAmount(baseAmount)
                .build();
    }

    /**
     * 할인 이력 생성 팩토리 메서드 -> 결제 수단 할인용
     */
    public static DiscountHistory createPaymentMethodDiscount(
            Payment payment,
            String policyName,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            PaymentMethod paymentMethod,
            Integer applyOrder,
            BigDecimal baseAmount
    ) {
        return DiscountHistory.builder()
                .payment(payment)
                .policyType("PAYMENT_METHOD_DISCOUNT")
                .policyName(policyName)
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .paymentMethod(paymentMethod)
                .applyOrder(applyOrder)
                .baseAmount(baseAmount)
                .build();
    }
}
