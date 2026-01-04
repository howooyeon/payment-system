package com.polycube.dto;

import com.polycube.domain.enums.MemberGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "회원 등급은 필수입니다.")
    private MemberGrade grade;
}
