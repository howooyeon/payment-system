package com.polycube.domain;

import com.polycube.domain.enums.PaymentMethod;
import com.polycube.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiscountHistory> discountHistories = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 결제만 승인할 수 있습니다.");
        }
        this.status = PaymentStatus.APPROVED;
    }

    public void cancel() {
        if (this.status == PaymentStatus.REFUNDED || this.status == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소되거나 환불된 결제입니다.");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    public void refund() {
        if (this.status != PaymentStatus.APPROVED) {
            throw new IllegalStateException("승인된 결제만 환불할 수 있습니다.");
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
