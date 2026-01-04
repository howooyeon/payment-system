package com.polycube.controller;

import com.polycube.dto.MemberRequest;
import com.polycube.dto.MemberResponse;
import com.polycube.service.MemberService;
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

@Tag(name = "회원 API", description = "회원 생성, 조회 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 생성", description = "새로운 회원을 생성합니다.")
    @PostMapping
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberRequest request) {
        log.info("회원 생성 API 호출: name={}", request.getName());
        MemberResponse response = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "회원 조회", description = "ID로 회원 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(
            @Parameter(description = "회원 ID", required = true) @PathVariable Long id) {
        log.info("회원 조회 API 호출: memberId={}", id);
        MemberResponse response = memberService.getMember(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전체 회원 조회", description = "전체 회원 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAllMembers() {
        log.info("전체 회원 목록 조회 API 호출");
        List<MemberResponse> responses = memberService.getAllMembers();
        return ResponseEntity.ok(responses);
    }
}
