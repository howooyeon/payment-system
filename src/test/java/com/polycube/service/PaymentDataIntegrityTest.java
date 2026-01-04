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
 * 데이터 정합성 테스트
 *
 * 테스트 시나리오
 * 1. 할인 정책 변경 후 과거 결제 데이터 보존 검증
 * 2. 회원 등급 변경 후 과거 결제 이력 보존 검증
 * 3. 할인 이력의 불변성 검증
 */
@SpringBootTest
@Transactional
public class PaymentDataIntegrityTest {

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

    private Member member;
    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성 (초기 등급: VIP)
        member = Member.builder()
                .name("테스트 회원")
                .grade(MemberGrade.VIP)
                .build();
        memberRepository.save(member);

        // 테스트용 주문 생성
        order1 = Order.builder()
                .productName("상품 1")
                .originalPrice(new BigDecimal("10000"))
                .member(member)
                .build();
        orderRepository.save(order1);

        order2 = Order.builder()
                .productName("상품 2")
                .originalPrice(new BigDecimal("20000"))
                .member(member)
                .build();
        orderRepository.save(order2);
    }

    @Test
    @DisplayName("회원 등급 변경 후에도 과거 결제의 할인 이력은 당시 등급으로 보존된다")
    void testDiscountHistoryPreservesOriginalGrade() {
        // Given: VIP 회원이 포인트로 10,000원 결제
        PaymentRequest request1 = PaymentRequest.builder()
                .orderId(order1.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        // When: 첫 번째 결제 생성 (VIP 등급으로)
        PaymentResponse payment1 = paymentService.createPayment(request1);

        // 결제 생성 시점의 할인 이력 확인
        List<DiscountHistory> histories1 = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(payment1.getId());

        // VIP 등급 할인이 적용되었는지 확인
        DiscountHistory gradeDiscount1 = histories1.stream()
                .filter(h -> "GRADE_DISCOUNT".equals(h.getPolicyType()))
                .findFirst()
                .orElseThrow();

        assertThat(gradeDiscount1.getMemberGradeAtPayment()).isEqualTo(MemberGrade.VIP);
        assertThat(gradeDiscount1.getPolicyName()).isEqualTo("VIP 1,000원 고정 할인");
        assertThat(gradeDiscount1.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000"));

        // When: 회원 등급을 VVIP로 변경
        member.upgradeGrade(MemberGrade.VVIP);
        memberRepository.save(member);

        // 두 번째 결제 생성 (VVIP 등급으로)
        PaymentRequest request2 = PaymentRequest.builder()
                .orderId(order2.getId())
                .amount(new BigDecimal("20000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse payment2 = paymentService.createPayment(request2);

        // Then: 과거 결제의 할인 이력은 변경되지 않고 VIP로 보존
        List<DiscountHistory> historiesAfterUpgrade = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(payment1.getId());

        DiscountHistory preservedGradeDiscount = historiesAfterUpgrade.stream()
                .filter(h -> "GRADE_DISCOUNT".equals(h.getPolicyType()))
                .findFirst()
                .orElseThrow();

        assertThat(preservedGradeDiscount.getMemberGradeAtPayment()).isEqualTo(MemberGrade.VIP);
        assertThat(preservedGradeDiscount.getPolicyName()).isEqualTo("VIP 1,000원 고정 할인");
        assertThat(preservedGradeDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000"));

        // 새로운 결제는 VVIP 등급 할인이 적용됨
        List<DiscountHistory> histories2 = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(payment2.getId());

        DiscountHistory newGradeDiscount = histories2.stream()
                .filter(h -> "GRADE_DISCOUNT".equals(h.getPolicyType()))
                .findFirst()
                .orElseThrow();

        assertThat(newGradeDiscount.getMemberGradeAtPayment()).isEqualTo(MemberGrade.VVIP);
        assertThat(newGradeDiscount.getPolicyName()).isEqualTo("VVIP 10% 할인");
        assertThat(newGradeDiscount.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("결제 완료 후 할인 이력은 불변이며 삭제되지 않는다")
    void testDiscountHistoryIsImmutable() {
        // Given: 결제 생성
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order1.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse response = paymentService.createPayment(request);

        // When: 할인 이력 조회
        List<DiscountHistory> historiesBefore = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        // 할인 이력 개수 및 내용 저장
        int historyCount = historiesBefore.size();
        DiscountHistory gradeDiscountBefore = historiesBefore.get(0);
        String policyNameBefore = gradeDiscountBefore.getPolicyName();
        BigDecimal discountAmountBefore = gradeDiscountBefore.getDiscountAmount();
        MemberGrade gradeBefore = gradeDiscountBefore.getMemberGradeAtPayment();

        // 회원 등급 변경 (시뮬레이션: 정책 변경)
        member.upgradeGrade(MemberGrade.VVIP);
        memberRepository.save(member);

        // Then: 과거 할인 이력은 그대로 보존
        List<DiscountHistory> historiesAfter = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        assertThat(historiesAfter).hasSize(historyCount);

        DiscountHistory gradeDiscountAfter = historiesAfter.get(0);
        assertThat(gradeDiscountAfter.getPolicyName()).isEqualTo(policyNameBefore);
        assertThat(gradeDiscountAfter.getDiscountAmount()).isEqualByComparingTo(discountAmountBefore);
        assertThat(gradeDiscountAfter.getMemberGradeAtPayment()).isEqualTo(gradeBefore);
    }

    @Test
    @DisplayName("결제 승인 후에도 할인 이력이 유지된다")
    void testDiscountHistoryPreservedAfterApproval() {
        // Given: 결제 생성
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order1.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse createdPayment = paymentService.createPayment(request);

        // 할인 이력 조회
        List<DiscountHistory> historiesBefore = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(createdPayment.getId());
        assertThat(historiesBefore).hasSize(2); // 등급 할인 + 결제 수단 할인

        // When: 결제 승인
        PaymentResponse approvedPayment = paymentService.approvePayment(createdPayment.getId());

        // Then: 결제 상태는 변경되지만 할인 이력은 그대로 유지
        assertThat(approvedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        List<DiscountHistory> historiesAfter = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(approvedPayment.getId());

        assertThat(historiesAfter).hasSize(2);
        assertThat(historiesAfter.get(0).getPolicyType()).isEqualTo("GRADE_DISCOUNT");
        assertThat(historiesAfter.get(1).getPolicyType()).isEqualTo("PAYMENT_METHOD_DISCOUNT");
    }

    @Test
    @DisplayName("여러 번의 등급 변경 후에도 각 결제의 할인 이력은 당시 등급으로 보존된다")
    void testMultipleGradeChangesPreserveHistory() {
        // Given: NORMAL -> VIP -> VVIP 순서로 등급 변경하며 각각 결제 생성

        // 1. NORMAL 등급으로 초기화
        member.upgradeGrade(MemberGrade.NORMAL);
        memberRepository.save(member);

        Order normalOrder = Order.builder()
                .productName("NORMAL 상품")
                .originalPrice(new BigDecimal("10000"))
                .member(member)
                .build();
        orderRepository.save(normalOrder);

        PaymentRequest normalRequest = PaymentRequest.builder()
                .orderId(normalOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse normalPayment = paymentService.createPayment(normalRequest);

        // 2. VIP로 변경 후 결제
        member.upgradeGrade(MemberGrade.VIP);
        memberRepository.save(member);

        Order vipOrder = Order.builder()
                .productName("VIP 상품")
                .originalPrice(new BigDecimal("10000"))
                .member(member)
                .build();
        orderRepository.save(vipOrder);

        PaymentRequest vipRequest = PaymentRequest.builder()
                .orderId(vipOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse vipPayment = paymentService.createPayment(vipRequest);

        // 3. VVIP로 변경 후 결제
        member.upgradeGrade(MemberGrade.VVIP);
        memberRepository.save(member);

        Order vvipOrder = Order.builder()
                .productName("VVIP 상품")
                .originalPrice(new BigDecimal("10000"))
                .member(member)
                .build();
        orderRepository.save(vvipOrder);

        PaymentRequest vvipRequest = PaymentRequest.builder()
                .orderId(vvipOrder.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse vvipPayment = paymentService.createPayment(vvipRequest);

        // Then: 각 결제의 할인 이력은 당시 등급으로 보존
        // NORMAL 등급 결제: 등급 할인 없음, 결제 수단 할인만
        List<DiscountHistory> normalHistories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(normalPayment.getId());
        assertThat(normalHistories).hasSize(1); // 결제 수단 할인만
        assertThat(normalHistories.get(0).getPolicyType()).isEqualTo("PAYMENT_METHOD_DISCOUNT");

        // VIP 등급 결제: VIP 할인 + 결제 수단 할인
        List<DiscountHistory> vipHistories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(vipPayment.getId());
        assertThat(vipHistories).hasSize(2);
        assertThat(vipHistories.get(0).getMemberGradeAtPayment()).isEqualTo(MemberGrade.VIP);
        assertThat(vipHistories.get(0).getPolicyName()).isEqualTo("VIP 1,000원 고정 할인");

        // VVIP 등급 결제: VVIP 할인 + 결제 수단 할인
        List<DiscountHistory> vvipHistories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(vvipPayment.getId());
        assertThat(vvipHistories).hasSize(2);
        assertThat(vvipHistories.get(0).getMemberGradeAtPayment()).isEqualTo(MemberGrade.VVIP);
        assertThat(vvipHistories.get(0).getPolicyName()).isEqualTo("VVIP 10% 할인");
    }

    @Test
    @DisplayName("결제 데이터와 할인 이력은 트랜잭션으로 함께 저장되어 정합성을 보장한다")
    void testPaymentAndHistoryConsistency() {
        // Given & When: 결제 생성
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order1.getId())
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.POINT)
                .build();

        PaymentResponse response = paymentService.createPayment(request);

        // Then: 결제와 할인 이력이 모두 존재
        Payment payment = paymentRepository.findById(response.getId()).orElseThrow();
        List<DiscountHistory> histories = discountHistoryRepository
                .findByPaymentIdOrderByApplyOrder(response.getId());

        assertThat(payment).isNotNull();
        assertThat(histories).isNotEmpty();

        // 할인 이력의 총 할인 금액 = 결제의 총 할인 금액
        BigDecimal totalHistoryDiscount = histories.stream()
                .map(DiscountHistory::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalHistoryDiscount).isEqualByComparingTo(payment.getDiscountAmount());
    }
}
