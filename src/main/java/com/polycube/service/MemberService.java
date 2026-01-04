package com.polycube.service;

import com.polycube.domain.Member;
import com.polycube.dto.MemberRequest;
import com.polycube.dto.MemberResponse;
import com.polycube.exception.MemberNotFoundException;
import com.polycube.repository.MemberRepository;
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
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 회원 생성
     */
    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        log.info("회원 생성 요청: name={}, grade={}",
                request.getName(), request.getGrade());

        Member member = Member.builder()
                .name(request.getName())
                .grade(request.getGrade())
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("회원 생성 완료: memberId={}", savedMember.getId());

        return MemberResponse.from(savedMember);
    }

    /**
     * 회원 단건 조회
     */
    public MemberResponse getMember(Long memberId) {
        log.info("회원 조회: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        return MemberResponse.from(member);
    }

    /**
     * 전체 회원 목록 조회
     */
    public List<MemberResponse> getAllMembers() {
        log.info("전체 회원 목록 조회");

        return memberRepository.findAll().stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }
}
