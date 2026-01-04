package com.polycube.service;

import com.polycube.domain.Payment;
import com.polycube.domain.enums.PaymentMethod;
import com.polycube.domain.enums.PaymentStatus;
import com.polycube.dto.PaymentRequest;
import com.polycube.dto.PaymentResponse;
import com.polycube.exception.InvalidPaymentStateException;
import com.polycube.exception.PaymentNotFoundException;
import com.polycube.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequest = PaymentRequest.builder()
                .orderId(1L)
                .amount(new BigDecimal("10000.00"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("테스트 결제")
                .build();

        payment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("10000.00"))
                .currency("KRW")
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("테스트 결제")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 할인 정책이 적용된 결제 생성 테스트는 PaymentServiceWithDiscountTest에서 수행
    // @Test
    // @DisplayName("결제 생성 성공")
    // void createPayment_Success() {
    //     // given
    //     when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
    //
    //     // when
    //     PaymentResponse response = paymentService.createPayment(paymentRequest);
    //
    //     // then
    //     assertThat(response).isNotNull();
    //     assertThat(response.getUserId()).isEqualTo("user123");
    //     assertThat(response.getAmount()).isEqualTo(new BigDecimal("10000.00"));
    //     assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
    //
    //     verify(paymentRepository, times(1)).save(any(Payment.class));
    // }

    @Test
    @DisplayName("결제 승인 성공")
    void approvePayment_Success() {
        // given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // when
        PaymentResponse response = paymentService.approvePayment(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제를 찾을 수 없음")
    void approvePayment_NotFound() {
        // given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.approvePayment(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("결제를 찾을 수 없습니다");

        verify(paymentRepository, times(1)).findById(999L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 승인 실패 - 이미 승인된 결제")
    void approvePayment_AlreadyApproved() {
        // given
        Payment approvedPayment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("10000.00"))
                .currency("KRW")
                .status(PaymentStatus.APPROVED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(approvedPayment));

        // when & then
        assertThatThrownBy(() -> paymentService.approvePayment(1L))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("PENDING 상태의 결제만 승인할 수 있습니다");

        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("결제 취소 성공")
    void cancelPayment_Success() {
        // given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // when
        PaymentResponse response = paymentService.cancelPayment(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 환불 성공")
    void refundPayment_Success() {
        // given
        Payment approvedPayment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("10000.00"))
                .currency("KRW")
                .status(PaymentStatus.APPROVED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(approvedPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(approvedPayment);

        // when
        PaymentResponse response = paymentService.refundPayment(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 환불 실패 - PENDING 상태의 결제")
    void refundPayment_InvalidState() {
        // given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.refundPayment(1L))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("승인된 결제만 환불할 수 있습니다");

        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("결제 단건 조회 성공")
    void getPayment_Success() {
        // given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // when
        PaymentResponse response = paymentService.getPayment(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("전체 결제 목록 조회")
    void getAllPayments_Success() {
        // given
        Payment payment2 = Payment.builder()
                .id(2L)
                .amount(new BigDecimal("20000.00"))
                .currency("KRW")
                .status(PaymentStatus.APPROVED)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findAll()).thenReturn(Arrays.asList(payment, payment2));

        // when
        List<PaymentResponse> responses = paymentService.getAllPayments();

        // then
        assertThat(responses).hasSize(2);

        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("상태별 결제 목록 조회")
    void getPaymentsByStatus_Success() {
        // given
        when(paymentRepository.findByStatus(PaymentStatus.PENDING))
                .thenReturn(Arrays.asList(payment));

        // when
        List<PaymentResponse> responses = paymentService.getPaymentsByStatus(PaymentStatus.PENDING);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(paymentRepository, times(1)).findByStatus(PaymentStatus.PENDING);
    }
}
