package com.polycube.repository;

import com.polycube.domain.Payment;
import com.polycube.domain.enums.PaymentMethod;
import com.polycube.domain.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("PaymentRepository 테스트")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        payment = Payment.builder()
                .amount(new BigDecimal("10000.00"))
                .currency("KRW")
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("테스트 결제")
                .build();
    }

    @Test
    @DisplayName("결제 저장 및 조회")
    void saveAndFind() {
        // when
        Payment savedPayment = paymentRepository.save(payment);

        // then
        assertThat(savedPayment.getId()).isNotNull();
        assertThat(savedPayment.getAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("상태로 결제 조회")
    void findByStatus() {
        // given
        paymentRepository.save(payment);

        Payment payment2 = Payment.builder()
                .amount(new BigDecimal("20000.00"))
                .currency("KRW")
                .status(PaymentStatus.APPROVED)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();
        paymentRepository.save(payment2);

        // when
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);
        List<Payment> approvedPayments = paymentRepository.findByStatus(PaymentStatus.APPROVED);

        // then
        assertThat(pendingPayments).hasSize(1);
        assertThat(pendingPayments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);

        assertThat(approvedPayments).hasSize(1);
        assertThat(approvedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("결제 삭제")
    void deletePayment() {
        // given
        Payment savedPayment = paymentRepository.save(payment);

        // when
        paymentRepository.deleteById(savedPayment.getId());

        // then
        Optional<Payment> foundPayment = paymentRepository.findById(savedPayment.getId());
        assertThat(foundPayment).isEmpty();
    }

    @Test
    @DisplayName("전체 결제 수 조회")
    void countAll() {
        // given
        paymentRepository.save(payment);

        Payment payment2 = Payment.builder()
                .amount(new BigDecimal("20000.00"))
                .currency("KRW")
                .status(PaymentStatus.APPROVED)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();
        paymentRepository.save(payment2);

        // when
        long count = paymentRepository.count();

        // then
        assertThat(count).isEqualTo(2);
    }
}
