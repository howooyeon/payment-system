package com.polycube.service.discount;

import com.polycube.domain.Member;

import java.math.BigDecimal;

/**
 * 할인 정책 인터페이스
 * 새로운 할인 정책을 추가/변경할 수 있도록 확장 가능한 구조
 */
public interface DiscountPolicy {

    /**
     * 할인 금액을 계산합니다.
     *
     * @param member 회원 정보
     * @param price 원가
     * @return 할인 금액
     */
    BigDecimal discount(Member member, BigDecimal price);
}
