package com.polycube.service;

import com.polycube.domain.Member;
import com.polycube.domain.Order;
import com.polycube.dto.OrderRequest;
import com.polycube.dto.OrderResponse;
import com.polycube.exception.MemberNotFoundException;
import com.polycube.repository.MemberRepository;
import com.polycube.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    /**
     * 주문 생성
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("주문 생성 요청: memberId={}, productName={}, originalPrice={}",
                request.getMemberId(), request.getProductName(), request.getOriginalPrice());

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new MemberNotFoundException(request.getMemberId()));

        Order order = Order.builder()
                .productName(request.getProductName())
                .originalPrice(request.getOriginalPrice())
                .member(member)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("주문 생성 완료: orderId={}", savedOrder.getId());

        return OrderResponse.from(savedOrder);
    }

    /**
     * 주문 단건 조회
     */
    public OrderResponse getOrder(Long orderId) {
        log.info("주문 조회: orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        return OrderResponse.from(order);
    }

    /**
     * 전체 주문 목록 조회
     */
    public List<OrderResponse> getAllOrders() {
        log.info("전체 주문 목록 조회");

        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원별 주문 목록 조회
     */
    public List<OrderResponse> getOrdersByMemberId(Long memberId) {
        log.info("회원별 주문 목록 조회: memberId={}", memberId);

        return orderRepository.findByMemberId(memberId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }
}
