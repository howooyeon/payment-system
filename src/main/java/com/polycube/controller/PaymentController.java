package com.polycube.controller;

import com.polycube.domain.enums.PaymentStatus;
import com.polycube.dto.PaymentRequest;
import com.polycube.dto.PaymentResponse;
import com.polycube.service.PaymentService;
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

@Tag(name = "결제 API", description = "결제 생성, 승인, 취소, 환불, 조회 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 생성", description = "새로운 결제를 생성합니다. 회원 등급에 따라 할인이 자동으로 적용됩니다.")
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("결제 생성 API 호출: {}", request);
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "결제 승인", description = "PENDING 상태의 결제를 APPROVED 상태로 변경합니다.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<PaymentResponse> approvePayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable Long id) {
        log.info("결제 승인 API 호출: paymentId={}", id);
        PaymentResponse response = paymentService.approvePayment(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 취소", description = "PENDING 또는 APPROVED 상태의 결제를 CANCELLED 상태로 변경합니다.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable Long id) {
        log.info("결제 취소 API 호출: paymentId={}", id);
        PaymentResponse response = paymentService.cancelPayment(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 환불", description = "APPROVED 상태의 결제를 REFUNDED 상태로 변경합니다.")
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable Long id) {
        log.info("결제 환불 API 호출: paymentId={}", id);
        PaymentResponse response = paymentService.refundPayment(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 단건 조회", description = "ID로 결제 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable Long id) {
        log.info("결제 조회 API 호출: paymentId={}", id);
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 목록 조회", description = "전체 결제 목록을 조회합니다. memberId나 status로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
            @Parameter(description = "회원 ID (선택)", required = false) @RequestParam(required = false) Long memberId,
            @Parameter(description = "결제 상태 (선택)", required = false) @RequestParam(required = false) PaymentStatus status) {
        log.info("결제 목록 조회 API 호출: memberId={}, status={}", memberId, status);

        List<PaymentResponse> responses;

        if (memberId != null && status != null) {
            responses = paymentService.getPaymentsByMemberIdAndStatus(memberId, status);
        } else if (memberId != null) {
            responses = paymentService.getPaymentsByMemberId(memberId);
        } else if (status != null) {
            responses = paymentService.getPaymentsByStatus(status);
        } else {
            responses = paymentService.getAllPayments();
        }

        return ResponseEntity.ok(responses);
    }
}
