package com.polycube.service.discount;

import com.polycube.domain.Member;
import com.polycube.domain.enums.MemberGrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 비율 할인 정책
 * VVIP: 10% 할인
 */
@Component
public class PercentageDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal VVIP_DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public BigDecimal discount(Member member, BigDecimal price) {
        if (member.getGrade() == MemberGrade.VVIP) {
            return price.multiply(VVIP_DISCOUNT_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
