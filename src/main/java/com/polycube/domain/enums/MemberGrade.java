package com.polycube.domain.enums;

import lombok.Getter;

@Getter
public enum MemberGrade {
    NORMAL("normal"),
    VIP("VIP"),
    VVIP("VVIP");

    private final String description;

    MemberGrade(String description) {
        this.description = description;
    }

}
