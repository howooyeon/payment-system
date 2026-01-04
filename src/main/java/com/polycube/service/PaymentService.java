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
import com.polycube.exception.InvalidPaymentStateException;
import com.polycube.exception.OrderNotFoundException;
import com.polycube.exception.PaymentNotFoundException;
import com.polycube.repository.DiscountHistoryRepository;
import com.polycube.repository.OrderRepository;
import com.polycube.repository.PaymentRepository;
import com.polycube.service.discount.MemberGradeDiscountPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DiscountHistoryRepository discountHistoryRepository;
    private final MemberGradeDiscountPolicy discountPolicy;

    /**
     * 결제 생성 (할인 적용)
     * 1. 등급 할인 적용 (VVIP: 10%, VIP: 1000원)
     * 2. 결제 수단 할인 적용 (포인트: 추가 5%)
     * 3. 할인 이력 저장
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("결제 생성 요청: orderId={}, amount={}, currency={}, paymentMethod={}",
                request.getOrderId(), request.getAmount(), request.getCurrency(), request.getPaymentMethod());

        // 주문 조회
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        // 중복 결제 검증: 해당 주문에 이미 승인된 결제가 있는지 확인
        if (paymentRepository.existsByOrder_IdAndStatus(request.getOrderId(), PaymentStatus.APPROVED)) {
            log.warn("중복 결제 시도: orderId={}", request.getOrderId());
            throw new com.polycube.exception.DuplicatePaymentException(request.getOrderId());
        }

        // 주문으로부터 회원 정보 추출
        Member member = order.getMember();
        BigDecimal baseAmount = request.getAmount();

        // 1. 등급 할인 계산
        BigDecimal gradeDiscountAmount = discountPolicy.discount(member, baseAmount);
        BigDecimal amountAfterGradeDiscount = baseAmount.subtract(gradeDiscountAmount);

        log.info("등급 할인 적용: 원가={}, 등급할인금액={}, 등급할인후금액={}, 회원등급={}",
                baseAmount, gradeDiscountAmount, amountAfterGradeDiscount, member.getGrade());

        // 2. 결제 수단 할인 계산 (포인트 결제 시 추가 5% 할인)
        BigDecimal paymentMethodDiscountAmount = BigDecimal.ZERO;
        if (request.getPaymentMethod() == PaymentMethod.POINT) {
            paymentMethodDiscountAmount = amountAfterGradeDiscount
                    .multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.HALF_UP);
            log.info("결제 수단 할인 적용: 포인트 결제 5% 추가 할인={}", paymentMethodDiscountAmount);
        }

        // 최종 금액 계산
        BigDecimal totalDiscountAmount = gradeDiscountAmount.add(paymentMethodDiscountAmount);
        BigDecimal finalAmount = amountAfterGradeDiscount.subtract(paymentMethodDiscountAmount);

        log.info("최종 할인 적용: 총할인금액={}, 최종결제금액={}", totalDiscountAmount, finalAmount);

        // Payment 엔티티 생성
        Payment payment = Payment.builder()
                .order(order)
                .amount(baseAmount)
                .discountAmount(totalDiscountAmount)
                .finalAmount(finalAmount)
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .description(request.getDescription())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 3. 할인 이력 저장
        // 3-1. 등급 할인 이력 저장
        if (gradeDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            DiscountHistory gradeDiscountHistory = createGradeDiscountHistory(
                    savedPayment, member, baseAmount, gradeDiscountAmount);
            discountHistoryRepository.save(gradeDiscountHistory);
            log.info("등급 할인 이력 저장: policyName={}, amount={}",
                    gradeDiscountHistory.getPolicyName(), gradeDiscountAmount);
        }

        // 3-2. 결제 수단 할인 이력 저장
        if (paymentMethodDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            DiscountHistory paymentMethodDiscountHistory = DiscountHistory.createPaymentMethodDiscount(
                    savedPayment,
                    "포인트 결제 5% 추가 할인",
                    new BigDecimal("5.00"),
                    paymentMethodDiscountAmount,
                    request.getPaymentMethod(),
                    2,
                    amountAfterGradeDiscount
            );
            discountHistoryRepository.save(paymentMethodDiscountHistory);
            log.info("결제 수단 할인 이력 저장: policyName=포인트 결제 5% 추가 할인, amount={}",
                    paymentMethodDiscountAmount);
        }

        log.info("결제 생성 완료: paymentId={}", savedPayment.getId());

        return PaymentResponse.from(savedPayment);
    }

    /**
     * 등급 할인 이력 생성
     */
    private DiscountHistory createGradeDiscountHistory(Payment payment, Member member,
                                                       BigDecimal baseAmount, BigDecimal discountAmount) {
        MemberGrade grade = member.getGrade();

        if (grade == MemberGrade.VVIP) {
            return DiscountHistory.createGradeDiscount(
                    payment,
                    "VVIP 10% 할인",
                    new BigDecimal("10.00"),
                    discountAmount,
                    grade,
                    1,
                    baseAmount
            );
        } else if (grade == MemberGrade.VIP) {
            return DiscountHistory.createGradeDiscount(
                    payment,
                    "VIP 1,000원 고정 할인",
                    null,
                    discountAmount,
                    grade,
                    1,
                    baseAmount
            );
        }

        throw new IllegalStateException("할인이 적용되지 않는 등급입니다: " + grade);
    }

    /**
     * 결제 승인
     */
    @Transactional
    public PaymentResponse approvePayment(Long paymentId) {
        log.info("결제 승인 요청: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        try {
            payment.approve();
            Payment approvedPayment = paymentRepository.save(payment);
            log.info("결제 승인 완료: paymentId={}", paymentId);
            return PaymentResponse.from(approvedPayment);
        } catch (IllegalStateException e) {
            log.error("결제 승인 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw new InvalidPaymentStateException(e.getMessage());
        }
    }

    /**
     * 결제 취소
     */
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        log.info("결제 취소 요청: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        try {
            payment.cancel();
            Payment cancelledPayment = paymentRepository.save(payment);
            log.info("결제 취소 완료: paymentId={}", paymentId);
            return PaymentResponse.from(cancelledPayment);
        } catch (IllegalStateException e) {
            log.error("결제 취소 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw new InvalidPaymentStateException(e.getMessage());
        }
    }

    /**
     * 결제 환불
     */
    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        log.info("결제 환불 요청: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        try {
            payment.refund();
            Payment refundedPayment = paymentRepository.save(payment);
            log.info("결제 환불 완료: paymentId={}", paymentId);
            return PaymentResponse.from(refundedPayment);
        } catch (IllegalStateException e) {
            log.error("결제 환불 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw new InvalidPaymentStateException(e.getMessage());
        }
    }

    /**
     * 결제 단건 조회
     */
    public PaymentResponse getPayment(Long paymentId) {
        log.info("결제 조회: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return PaymentResponse.from(payment);
    }

    /**
     * 전체 결제 목록 조회
     */
    public List<PaymentResponse> getAllPayments() {
        log.info("전체 결제 목록 조회");

        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원별 결제 목록 조회
     */
    public List<PaymentResponse> getPaymentsByMemberId(Long memberId) {
        log.info("회원별 결제 목록 조회: memberId={}", memberId);

        return paymentRepository.findByOrder_Member_Id(memberId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 결제 목록 조회
     */
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        log.info("상태별 결제 목록 조회: status={}", status);

        return paymentRepository.findByStatus(status).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원별 & 상태별 결제 목록 조회
     */
    public List<PaymentResponse> getPaymentsByMemberIdAndStatus(Long memberId, PaymentStatus status) {
        log.info("회원별 & 상태별 결제 목록 조회: memberId={}, status={}", memberId, status);

        return paymentRepository.findByOrder_Member_IdAndStatus(memberId, status).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
}
