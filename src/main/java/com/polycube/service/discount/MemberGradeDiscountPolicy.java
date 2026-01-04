package com.polycube.service.discount;

import com.polycube.domain.Member;
import com.polycube.domain.enums.MemberGrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 회원 등급별 할인 정책 통합 관리
 * - NORMAL: 할인 없음
 * - VIP: 1,000원 고정 금액 할인
 * - VVIP: 10% 비율 할인
 */
@Component
public class MemberGradeDiscountPolicy implements DiscountPolicy {

    private final FixedAmountDiscountPolicy fixedAmountDiscountPolicy;
    private final PercentageDiscountPolicy percentageDiscountPolicy;

    public MemberGradeDiscountPolicy(
            FixedAmountDiscountPolicy fixedAmountDiscountPolicy,
            PercentageDiscountPolicy percentageDiscountPolicy) {
        this.fixedAmountDiscountPolicy = fixedAmountDiscountPolicy;
        this.percentageDiscountPolicy = percentageDiscountPolicy;
    }

    @Override
    public BigDecimal discount(Member member, BigDecimal price) {
        if (member.getGrade() == MemberGrade.VVIP) {
            return percentageDiscountPolicy.discount(member, price);
        } else if (member.getGrade() == MemberGrade.VIP) {
            return fixedAmountDiscountPolicy.discount(member, price);
        }
        // NORMAL 등급은 할인 없음
        return BigDecimal.ZERO;
    }
}
