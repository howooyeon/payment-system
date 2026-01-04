package com.polycube.service;

import com.polycube.domain.DiscountHistory;
import com.polycube.domain.Member;
import com.polycube.domain.Order;
import com.polycube.domain.Payment;
import com.polycube.domain.enums.MemberGrade;
import com.polycube.domain.enums.PaymentMethod;
import com.polycube.domain.enums.PaymentStatus;
import com.polycube.dto.PaymentRequest;
import com.polycube.dto.PaymentResponse;
import com.polycube.repository.DiscountHistoryRepository;
import com.polycube.repository.MemberRepository;
import com.polycube.repository.OrderRepository;
import com.polycube.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 중복 할인 우선순위 및 최종 금액 검증 테스트
 *
 * 테스트 시나리오
 * 1. 등급 할인 + 결제 수단 할인의 조합 테스트
 * 2. 할인 적용 순서 검증 (등급 할인 -> 결제 수단 할인)
 * 3. 최종 금액 정확성 검증
 */
@SpringBootTest
@Transactional
public class PaymentServiceMultipleDiscountTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DiscountHistoryRepository discountHistoryRepository;

    private Member vipMember;
    private Member vvipMember;
    private Member normalMember;
    private Order vipOrder;
    private Order vvipOrder;
    private Order normalOrder;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        vipMember = Member.builder()
                .name("VIP 회원")
                .grade(MemberGrade.VIP)
                .build();
        memberRepository.save(vipMember);

        vvipMember = Member.builder()
                .name("VVIP 회원")
                .grade(MemberGrade.VVIP)
                .build();
        memberRepository.save(vvipMember);

        normalMember = Member.builder()
                .name("일반 회원")
                .grade(MemberGrade.NORMAL)
                .build();
        memberRepository.save(normalMember);

        // 테스트용 주문 생성
        vipOrder = Order.builder()
                .productName("VIP 상품")
                .originalPrice(new BigDecimal("10000"))
                .member(vipMember)
                .build();
        orderRepository.save(vipOrder);

        vvipOrder = Order.builder()
                .productName("VVIP 상품")
                .originalPrice(new BigDecimal("10000"))
                .member(vvipMember)
                .build();
        orderRepository.save(vvipOrder);

        normalOrder = Order.builder()
                .productName("일반 상품")
                .originalPrice(new BigDecimal("10000"))
                .member(normalMember)
                .build();
        orderRepository.save(normalOrder);
    }

    @Test
    @DisplayName("VIP 회원 + 포인트 결제 = 1000원 고정 할인 + 5% 결제 수단 할인")
    void testVipMemberWithPointPayment() {
        // Given: VIP 회원이 포인트로 10,000원 결제
        PaymentRequest request = PaymentRequest.builder()
                .orderId(vipOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        // When: 결제 생성
        PaymentResponse response = paymentService.createPayment(request);

        // Then: 할인 금액 검증
        // 1. 등급 할인: 1,000원
        // 2. 등급 할인 후 금액: 10,000 - 1,000 = 9,000원
        // 3. 결제 수단 할인: 9,000 * 0.05 = 450원
        // 4. 최종 금액: 9,000 - 450 = 8,550원
        // 5. 총 할인 금액: 1,000 + 450 = 1,450원

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1450.00"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("8550.00"));

        // 할인 이력 검증
        List<DiscountHistory> histories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        assertThat(histories).hasSize(2);

        // 첫 번째 할인: 등급 할인 (VIP 1,000원)
        DiscountHistory gradeDiscount = histories.get(0);
        assertThat(gradeDiscount.getPolicyType()).isEqualTo("GRADE_DISCOUNT");
        assertThat(gradeDiscount.getPolicyName()).isEqualTo("VIP 1,000원 고정 할인");
        assertThat(gradeDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(gradeDiscount.getMemberGradeAtPayment()).isEqualTo(MemberGrade.VIP);
        assertThat(gradeDiscount.getApplyOrder()).isEqualTo(1);
        assertThat(gradeDiscount.getBaseAmount()).isEqualByComparingTo(new BigDecimal("10000"));

        // 두 번째 할인: 결제 수단 할인 (포인트 5%)
        DiscountHistory paymentMethodDiscount = histories.get(1);
        assertThat(paymentMethodDiscount.getPolicyType()).isEqualTo("PAYMENT_METHOD_DISCOUNT");
        assertThat(paymentMethodDiscount.getPolicyName()).isEqualTo("포인트 결제 5% 추가 할인");
        assertThat(paymentMethodDiscount.getDiscountRate()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(paymentMethodDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(paymentMethodDiscount.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
        assertThat(paymentMethodDiscount.getApplyOrder()).isEqualTo(2);
        assertThat(paymentMethodDiscount.getBaseAmount()).isEqualByComparingTo(new BigDecimal("9000"));
    }

    @Test
    @DisplayName("VVIP 회원 + 포인트 결제 = 10% 비율 할인 + 5% 결제 수단 할인")
    void testVvipMemberWithPointPayment() {
        // Given: VVIP 회원이 포인트로 10,000원 결제
        PaymentRequest request = PaymentRequest.builder()
                .orderId(vvipOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        // When: 결제 생성
        PaymentResponse response = paymentService.createPayment(request);

        // Then: 할인 금액 검증
        // 1. 등급 할인: 10,000 * 0.1 = 1,000원
        // 2. 등급 할인 후 금액: 10,000 - 1,000 = 9,000원
        // 3. 결제 수단 할인: 9,000 * 0.05 = 450원
        // 4. 최종 금액: 9,000 - 450 = 8,550원
        // 5. 총 할인 금액: 1,000 + 450 = 1,450원

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1450.00"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("8550.00"));

        // 할인 이력 검증
        List<DiscountHistory> histories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        assertThat(histories).hasSize(2);

        // 첫 번째 할인: 등급 할인 (VVIP 10%)
        DiscountHistory gradeDiscount = histories.get(0);
        assertThat(gradeDiscount.getPolicyType()).isEqualTo("GRADE_DISCOUNT");
        assertThat(gradeDiscount.getPolicyName()).isEqualTo("VVIP 10% 할인");
        assertThat(gradeDiscount.getDiscountRate()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(gradeDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(gradeDiscount.getMemberGradeAtPayment()).isEqualTo(MemberGrade.VVIP);
        assertThat(gradeDiscount.getApplyOrder()).isEqualTo(1);

        // 두 번째 할인: 결제 수단 할인 (포인트 5%)
        DiscountHistory paymentMethodDiscount = histories.get(1);
        assertThat(paymentMethodDiscount.getPolicyType()).isEqualTo("PAYMENT_METHOD_DISCOUNT");
        assertThat(paymentMethodDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(paymentMethodDiscount.getApplyOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("일반 회원 + 포인트 결제 = 등급 할인 없음 + 5% 결제 수단 할인만 적용")
    void testNormalMemberWithPointPayment() {
        // Given: 일반 회원이 포인트로 10,000원 결제
        PaymentRequest request = PaymentRequest.builder()
                .orderId(normalOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        // When: 결제 생성
        PaymentResponse response = paymentService.createPayment(request);

        // Then: 할인 금액 검증
        // 1. 등급 할인: 0원 (NORMAL 등급은 할인 없음)
        // 2. 등급 할인 후 금액: 10,000원
        // 3. 결제 수단 할인: 10,000 * 0.05 = 500원
        // 4. 최종 금액: 10,000 - 500 = 9,500원
        // 5. 총 할인 금액: 500원

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("9500.00"));

        // 할인 이력 검증 (등급 할인 이력은 없고, 결제 수단 할인만 존재)
        List<DiscountHistory> histories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        assertThat(histories).hasSize(1);

        // 결제 수단 할인만 존재
        DiscountHistory paymentMethodDiscount = histories.get(0);
        assertThat(paymentMethodDiscount.getPolicyType()).isEqualTo("PAYMENT_METHOD_DISCOUNT");
        assertThat(paymentMethodDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(paymentMethodDiscount.getBaseAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("VIP 회원 + 신용카드 결제 = 1000원 고정 할인만 적용")
    void testVipMemberWithCreditCard() {
        // Given: VIP 회원이 신용카드로 10,000원 결제
        PaymentRequest request = PaymentRequest.builder()
                .orderId(vipOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        // When: 결제 생성
        PaymentResponse response = paymentService.createPayment(request);

        // Then: 할인 금액 검증
        // 1. 등급 할인: 1,000원
        // 2. 결제 수단 할인: 0원 (신용카드는 추가 할인 없음)
        // 3. 최종 금액: 10,000 - 1,000 = 9,000원

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("9000"));

        // 할인 이력 검증 (등급 할인만 존재)
        List<DiscountHistory> histories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        assertThat(histories).hasSize(1);

        // 등급 할인만 존재
        DiscountHistory gradeDiscount = histories.get(0);
        assertThat(gradeDiscount.getPolicyType()).isEqualTo("GRADE_DISCOUNT");
        assertThat(gradeDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("할인 적용 순서 검증: 등급 할인 먼저, 결제 수단 할인은 등급 할인 후 금액에 적용")
    void testDiscountApplyOrder() {
        // Given: VVIP 회원이 포인트로 20,000원 결제
        PaymentRequest request = PaymentRequest.builder()
                .orderId(vvipOrder.getId())
                .amount(new BigDecimal("20000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        // When: 결제 생성
        PaymentResponse response = paymentService.createPayment(request);

        // Then: 할인 적용 순서 검증
        // 1. 등급 할인: 20,000 * 0.1 = 2,000원
        // 2. 등급 할인 후 금액: 18,000원
        // 3. 결제 수단 할인: 18,000 * 0.05 = 900원
        // 4. 최종 금액: 17,100원

        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("17100.00"));

        List<DiscountHistory> histories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        // 첫 번째 할인의 기준 금액은 원가
        assertThat(histories.get(0).getBaseAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(histories.get(0).getApplyOrder()).isEqualTo(1);

        // 두 번째 할인의 기준 금액은 등급 할인 후 금액
        assertThat(histories.get(1).getBaseAmount()).isEqualByComparingTo(new BigDecimal("18000"));
        assertThat(histories.get(1).getApplyOrder()).isEqualTo(2);
    }
}
