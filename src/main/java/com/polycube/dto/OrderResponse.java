package com.polycube.dto;

import com.polycube.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String productName;
    private BigDecimal originalPrice;
    private Long memberId;
    private String memberName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .productName(order.getProductName())
                .originalPrice(order.getOriginalPrice())
                .memberId(order.getMember().getId())
                .memberName(order.getMember().getName())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
