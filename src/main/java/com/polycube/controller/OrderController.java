package com.polycube.controller;

import com.polycube.dto.OrderRequest;
import com.polycube.dto.OrderResponse;
import com.polycube.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문 API", description = "주문 생성, 조회 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("주문 생성 API 호출: memberId={}, productName={}", request.getMemberId(), request.getProductName());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "주문 조회", description = "ID로 주문 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "주문 ID", required = true) @PathVariable Long id) {
        log.info("주문 조회 API 호출: orderId={}", id);
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전체 주문 조회", description = "전체 주문 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("전체 주문 목록 조회 API 호출");
        List<OrderResponse> responses = orderService.getAllOrders();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "회원별 주문 조회", description = "회원 ID로 주문 목록을 조회합니다.")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByMemberId(
            @Parameter(description = "회원 ID", required = true) @PathVariable Long memberId) {
        log.info("회원별 주문 목록 조회 API 호출: memberId={}", memberId);
        List<OrderResponse> responses = orderService.getOrdersByMemberId(memberId);
        return ResponseEntity.ok(responses);
    }
}
