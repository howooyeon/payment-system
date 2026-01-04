package com.polycube.dto;

import com.polycube.domain.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @NotNull(message = "결제 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "결제 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "통화는 필수입니다.")
    @Size(min = 3, max = 3, message = "통화 코드는 3자리여야 합니다.")
    private String currency;

    @NotNull(message = "결제 수단은 필수입니다.")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
    private String description;
}
