package com.polycube.service.discount;

import com.polycube.domain.Member;
import com.polycube.domain.enums.MemberGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MemberGradeDiscountPolicyTest {

    private MemberGradeDiscountPolicy discountPolicy;

    @BeforeEach
    void setUp() {
        FixedAmountDiscountPolicy fixedAmountDiscountPolicy = new FixedAmountDiscountPolicy();
        PercentageDiscountPolicy percentageDiscountPolicy = new PercentageDiscountPolicy();
        discountPolicy = new MemberGradeDiscountPolicy(fixedAmountDiscountPolicy, percentageDiscountPolicy);
    }

    @Test
    @DisplayName("NORMAL 등급 회원은 할인을 받지 못한다")
    void normalMemberNoDiscount() {
        // given
        Member normalMember = Member.builder()
                .name("일반회원")
                .grade(MemberGrade.NORMAL)
                .build();
        BigDecimal price = new BigDecimal("10000");

        // when
        BigDecimal discount = discountPolicy.discount(normalMember, price);

        // then
        assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("VIP 등급 회원은 1,000원 고정 할인을 받는다")
    void vipMemberFixedDiscount() {
        // given
        Member vipMember = Member.builder()
                .name("VIP회원")
                .grade(MemberGrade.VIP)
                .build();
        BigDecimal price = new BigDecimal("10000");

        // when
        BigDecimal discount = discountPolicy.discount(vipMember, price);

        // then
        assertThat(discount).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("VVIP 등급 회원은 10% 할인을 받는다")
    void vvipMemberPercentageDiscount() {
        // given
        Member vvipMember = Member.builder()
                .name("VVIP회원")
                .grade(MemberGrade.VVIP)
                .build();
        BigDecimal price = new BigDecimal("10000");

        // when
        BigDecimal discount = discountPolicy.discount(vvipMember, price);

        // then
        assertThat(discount).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("VVIP 등급 회원의 할인 금액은 주문 금액에 비례한다")
    void vvipMemberDiscountScalesWithPrice() {
        // given
        Member vvipMember = Member.builder()
                .name("VVIP회원")
                .grade(MemberGrade.VVIP)
                .build();
        BigDecimal price = new BigDecimal("50000");

        // when
        BigDecimal discount = discountPolicy.discount(vvipMember, price);

        // then
        assertThat(discount).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("VIP 등급 회원의 할인 금액은 주문 금액과 무관하게 1,000원이다")
    void vipMemberDiscountIsFixed() {
        // given
        Member vipMember = Member.builder()
                .name("VIP회원")
                .grade(MemberGrade.VIP)
                .build();
        BigDecimal price = new BigDecimal("50000");

        // when
        BigDecimal discount = discountPolicy.discount(vipMember, price);

        // then
        assertThat(discount).isEqualByComparingTo(new BigDecimal("1000"));
    }
}
