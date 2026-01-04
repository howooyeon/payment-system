package com.polycube.service;

import com.polycube.domain.Member;
import com.polycube.domain.Order;
import com.polycube.domain.Payment;
import com.polycube.domain.enums.PaymentStatus;
import com.polycube.dto.PaymentRequest;
import com.polycube.dto.PaymentResponse;
import com.polycube.exception.InvalidPaymentStateException;
import com.polycube.exception.OrderNotFoundException;
import com.polycube.exception.PaymentNotFoundException;
import com.polycube.repository.OrderRepository;
import com.polycube.repository.PaymentRepository;
import com.polycube.service.discount.MemberGradeDiscountPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MemberGradeDiscountPolicy discountPolicy;

    /**
     * 결제 생성 (할인 적용)
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("결제 생성 요청: orderId={}, amount={}, currency={}",
                request.getOrderId(), request.getAmount(), request.getCurrency());

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

        // 할인 금액 계산
        BigDecimal discountAmount = discountPolicy.discount(member, request.getAmount());
        BigDecimal finalAmount = request.getAmount().subtract(discountAmount);

        log.info("할인 적용: 원가={}, 할인금액={}, 최종금액={}, 회원등급={}, memberId={}",
                request.getAmount(), discountAmount, finalAmount, member.getGrade(), member.getId());

        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .description(request.getDescription())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("결제 생성 완료: paymentId={}", savedPayment.getId());

        return PaymentResponse.from(savedPayment);
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
