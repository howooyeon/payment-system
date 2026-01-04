package com.polycube.repository;

import com.polycube.domain.Payment;
import com.polycube.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 특정 회원의 모든 결제 내역 조회
     */
    List<Payment> findByOrder_Member_Id(Long memberId);

    /**
     * 특정 상태의 모든 결제 내역 조회
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * 특정 회원의 특정 상태 결제 내역 조회
     */
    List<Payment> findByOrder_Member_IdAndStatus(Long memberId, PaymentStatus status);

    /**
     * 특정 주문에 대한 승인된 결제가 존재하는지 확인
     */
    boolean existsByOrder_IdAndStatus(Long orderId, PaymentStatus status);

    /**
     * 특정 주문의 모든 결제 내역 조회
     */
    List<Payment> findByOrder_Id(Long orderId);
}
