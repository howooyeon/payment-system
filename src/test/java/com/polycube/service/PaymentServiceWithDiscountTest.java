package com.polycube.service;

import com.polycube.domain.Member;
import com.polycube.domain.Order;
import com.polycube.domain.Payment;
import com.polycube.domain.enums.MemberGrade;
import com.polycube.domain.enums.PaymentMethod;
import com.polycube.domain.enums.PaymentStatus;
import com.polycube.dto.PaymentRequest;
import com.polycube.dto.PaymentResponse;
import com.polycube.exception.OrderNotFoundException;
import com.polycube.repository.DiscountHistoryRepository;
import com.polycube.repository.OrderRepository;
import com.polycube.repository.PaymentRepository;
import com.polycube.service.discount.MemberGradeDiscountPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceWithDiscountTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DiscountHistoryRepository discountHistoryRepository;

    @Mock
    private MemberGradeDiscountPolicy discountPolicy;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("NORMAL 등급 회원의 결제는 할인이 적용되지 않는다")
    void createPaymentForNormalMember() {
        // given
        Member normalMember = createMember(1L, "user1", "일반회원", MemberGrade.NORMAL);
        Order order = createOrder(1L, "상품A", new BigDecimal("10000"), normalMember);

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("상품 구매")
                .build();

        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(discountPolicy.discount(normalMember, new BigDecimal("10000")))
                .willReturn(BigDecimal.ZERO);

        Payment savedPayment = Payment.builder()
                .id(1L)
                .order(order)
                .amount(new BigDecimal("10000"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("상품 구매")
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentResponse response = paymentService.createPayment(request);

        // then
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        verify(orderRepository).findById(1L);
        verify(discountPolicy).discount(normalMember, new BigDecimal("10000"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("VIP 등급 회원의 결제는 1,000원 할인이 적용된다")
    void createPaymentForVipMember() {
        // given
        Member vipMember = createMember(2L, "user2", "VIP회원", MemberGrade.VIP);
        Order order = createOrder(2L, "상품B", new BigDecimal("10000"), vipMember);

        PaymentRequest request = PaymentRequest.builder()
                .orderId(2L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("상품 구매")
                .build();

        given(orderRepository.findById(2L)).willReturn(Optional.of(order));
        given(discountPolicy.discount(vipMember, new BigDecimal("10000")))
                .willReturn(new BigDecimal("1000"));

        Payment savedPayment = Payment.builder()
                .id(2L)
                .order(order)
                .amount(new BigDecimal("10000"))
                .discountAmount(new BigDecimal("1000"))
                .finalAmount(new BigDecimal("9000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("상품 구매")
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentResponse response = paymentService.createPayment(request);

        // then
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("9000"));
        verify(orderRepository).findById(2L);
        verify(discountPolicy).discount(vipMember, new BigDecimal("10000"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("VVIP 등급 회원의 결제는 10% 할인이 적용된다")
    void createPaymentForVvipMember() {
        // given
        Member vvipMember = createMember(3L, "user3", "VVIP회원", MemberGrade.VVIP);
        Order order = createOrder(3L, "상품C", new BigDecimal("10000"), vvipMember);

        PaymentRequest request = PaymentRequest.builder()
                .orderId(3L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .description("상품 구매")
                .build();

        given(orderRepository.findById(3L)).willReturn(Optional.of(order));
        given(discountPolicy.discount(vvipMember, new BigDecimal("10000")))
                .willReturn(new BigDecimal("1000.00"));

        Payment savedPayment = Payment.builder()
                .id(3L)
                .order(order)
                .amount(new BigDecimal("10000"))
                .discountAmount(new BigDecimal("1000.00"))
                .finalAmount(new BigDecimal("9000.00"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .description("상품 구매")
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentResponse response = paymentService.createPayment(request);

        // then
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("9000.00"));
        verify(orderRepository).findById(3L);
        verify(discountPolicy).discount(vvipMember, new BigDecimal("10000"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 결제 생성 시 예외 발생")
    void createPaymentWithInvalidOrderId() {
        // given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(999L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("상품 구매")
                .build();

        given(orderRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("주문을 찾을 수 없습니다: 999");
    }

    private Member createMember(Long id, String name, String unusedParam, MemberGrade grade) {
        return Member.builder()
                .id(id)
                .name(name)
                .grade(grade)
                .build();
    }

    private Order createOrder(Long id, String productName, BigDecimal originalPrice, Member member) {
        return Order.builder()
                .id(id)
                .productName(productName)
                .originalPrice(originalPrice)
                .member(member)
                .build();
    }
}
