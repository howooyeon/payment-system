package com.polycube.service.discount;

import com.polycube.domain.Member;
import com.polycube.domain.enums.MemberGrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 고정 금액 할인 정책
 * VIP: 1,000원 할인
 */
@Component
public class FixedAmountDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal VIP_DISCOUNT_AMOUNT = new BigDecimal("1000");

    @Override
    public BigDecimal discount(Member member, BigDecimal price) {
        if (member.getGrade() == MemberGrade.VIP) {
            return VIP_DISCOUNT_AMOUNT;
        }
        return BigDecimal.ZERO;
    }
}
