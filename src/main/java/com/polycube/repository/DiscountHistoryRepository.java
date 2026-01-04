package com.polycube.repository;

import com.polycube.domain.DiscountHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountHistoryRepository extends JpaRepository<DiscountHistory, Long> {

    /**
     * 특정 결제의 모든 할인 이력 조회 (적용 순서대로 정렬)
     */
    List<DiscountHistory> findByPaymentIdOrderByApplyOrder(Long paymentId);

    /**
     * 특정 결제 수단에 대한 모든 할인 이력 조회
     */
    List<DiscountHistory> findByPaymentMethod(com.polycube.domain.enums.PaymentMethod paymentMethod);

    /**
     * 특정 정책 타입의 모든 할인 이력 조회
     */
    List<DiscountHistory> findByPolicyType(String policyType);
}
