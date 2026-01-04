package com.polycube.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String productName;

    @NotNull(message = "원가는 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "원가는 0보다 커야 합니다.")
    private BigDecimal originalPrice;

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId;
}
